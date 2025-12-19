package se.sundsvall.postportalservice.service;

import static java.util.Collections.emptyList;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import generated.se.sundsvall.citizen.CitizenExtended;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.postportalservice.api.model.KivraEligibilityRequest;
import se.sundsvall.postportalservice.api.model.PrecheckCsvResponse;
import se.sundsvall.postportalservice.api.model.PrecheckResponse;
import se.sundsvall.postportalservice.api.model.PrecheckResponse.DeliveryMethod;
import se.sundsvall.postportalservice.api.model.PrecheckResponse.PrecheckRecipient;
import se.sundsvall.postportalservice.integration.citizen.CitizenIntegration;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.digitalregisteredletter.DigitalRegisteredLetterIntegration;
import se.sundsvall.postportalservice.integration.messaging.MessagingIntegration;
import se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration;
import se.sundsvall.postportalservice.service.mapper.EntityMapper;
import se.sundsvall.postportalservice.service.mapper.PrecheckMapper;
import se.sundsvall.postportalservice.service.util.CsvUtil;
import se.sundsvall.postportalservice.service.util.CitizenCategorizationHelper;
import se.sundsvall.postportalservice.service.util.PartyIdMappingHelper;
import se.sundsvall.postportalservice.service.util.PrecheckUtil;

@Service
public class PrecheckService {

	public static final String FAILURE_REASON_PARTY_ID_NOT_FOUND = "Party ID not found.";
	public static final String FAILURE_REASON_UNKNOWN_ERROR = "Unknown error.";

	private final DigitalRegisteredLetterIntegration digitalRegisteredLetterIntegration;
	private final CitizenIntegration citizenIntegration;
	private final MessagingSettingsIntegration messagingSettingsIntegration;
	private final MessagingIntegration messagingIntegration;
	private final PrecheckMapper precheckMapper;
	private final EmployeeService employeeService;
	private final EntityMapper entityMapper;

	public PrecheckService(
		final DigitalRegisteredLetterIntegration digitalRegisteredLetterIntegration,
		final CitizenIntegration citizenIntegration,
		final MessagingSettingsIntegration messagingSettingsIntegration,
		final MessagingIntegration messagingIntegration,
		final PrecheckMapper precheckMapper,
		final EmployeeService employeeService,
		final EntityMapper entityMapper) {
		this.digitalRegisteredLetterIntegration = digitalRegisteredLetterIntegration;
		this.citizenIntegration = citizenIntegration;
		this.messagingSettingsIntegration = messagingSettingsIntegration;
		this.messagingIntegration = messagingIntegration;
		this.precheckMapper = precheckMapper;
		this.employeeService = employeeService;
		this.entityMapper = entityMapper;
	}

	public PrecheckCsvResponse precheckCSV(final MultipartFile csvFile) {

		// Returns a map with personal identity numbers as keys and their occurrence counts as values
		var occurrenceMap = CsvUtil.validateCSV(csvFile);

		// Filter the map to include only entries with more than one occurrence
		var duplicateEntriesMap = occurrenceMap.entrySet().stream()
			.filter(entry -> entry.getValue() > 1)
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		return new PrecheckCsvResponse(duplicateEntriesMap);
	}

	public PrecheckResponse precheckPartyIds(final String municipalityId, final List<String> partyIds) {
		// Check if we can send using digital mail
		final var mailboxStatus = checkMailboxStatus(municipalityId, partyIds);

		// Get citizen details for those without digital mailboxes
		final var citizens = citizenIntegration.getCitizens(municipalityId, mailboxStatus.unreachable());

		// Categorize by registration status (TODO: age verification, need to call party to get legalIds)
		final var eligiblePartyIds = precheckMapper.toSnailMailEligiblePartyIds(
			citizens, citizenIntegration::isRegisteredInSweden);

		final var ineligiblePartyIds = precheckMapper.toSnailMailIneligiblePartyIds(
			citizens, citizenIntegration::isRegisteredInSweden);

		// Build precheck recipients with reason data
		return createPrecheckResponse(
			mailboxStatus.reachable(),
			eligiblePartyIds,
			ineligiblePartyIds,
			mailboxStatus.unreachableWithReason());
	}

	public List<RecipientEntity> precheckLegalIds(final String municipalityId, final List<String> legalIds) {
		if (legalIds == null || legalIds.isEmpty()) {
			return emptyList();
		}

		// Get partyIds and create mapping that we can use for age verification later on
		final var batches = citizenIntegration.getPartyIds(municipalityId, legalIds);
		final var partyIdMapping = PartyIdMappingHelper.extractPartyIdMapping(batches);

		// Check digital mailbox availability
		final var mailboxStatus = checkMailboxStatus(municipalityId, partyIdMapping.partyIds());

		List<CitizenExtended> citizens = new ArrayList<>();
		// Get citizen details for those without digital mailboxes
		if (!mailboxStatus.unreachable().isEmpty()) {  // No need to call if we have no unreachable partyIds
			citizens = citizenIntegration.getCitizens(municipalityId, mailboxStatus.unreachable());
		}

		// Categorize citizens by eligibility (age and registration)
		final var categorized = CitizenCategorizationHelper.categorizeCitizens(
			citizens, partyIdMapping.partyIdToLegalId(), citizenIntegration::isRegisteredInSweden);

		// Convert categorized citizens to recipient entities
		return createRecipientEntities(mailboxStatus.reachable(), categorized);
	}

	public List<String> precheckKivra(final String municipalityId, final KivraEligibilityRequest request) {
		return digitalRegisteredLetterIntegration.checkKivraEligibility(municipalityId, request.getPartyIds());
	}

	/**
	 * Checks mailbox status for given partyIds.
	 *
	 * @param  municipalityId the municipalityId
	 * @param  partyIds       list of partyIds to check
	 * @return                mailbox status with reachable and unreachable partyIds
	 */
	private MailboxStatus checkMailboxStatus(final String municipalityId, final List<String> partyIds) {
		final var sentBy = employeeService.getSentBy(municipalityId);
		final var orgNumber = messagingSettingsIntegration.getOrganizationNumber(municipalityId, sentBy.departmentId());
		final var mailboxes = messagingIntegration.precheckMailboxes(municipalityId, orgNumber, partyIds);

		final var reachable = PrecheckUtil.filterReachableMailboxes(mailboxes);
		final var unreachableWithReason = PrecheckUtil.filterUnreachableMailboxesWithReason(mailboxes);

		return new MailboxStatus(reachable, unreachableWithReason);
	}

	/**
	 * Builds recipient entities from categorized data.
	 *
	 * @param  digitalMailPartyIds partyIds eligible for digital mail
	 * @param  categorized         categorized citizens
	 * @return                     list of recipient entities
	 */
	private List<RecipientEntity> createRecipientEntities(
		final List<String> digitalMailPartyIds,
		final CitizenCategorizationHelper.CategorizedCitizens categorized) {

		final var digitalMailRecipients = createDigitalMailRecipients(digitalMailPartyIds);
		final var snailMailRecipients = createSnailMailRecipients(categorized.eligibleAdults());
		final var ineligibleMinorRecipients = createIneligibleMinorRecipients(categorized.ineligibleMinors());
		final var undeliverableRecipients = createUndeliverableRecipients(categorized.notRegisteredInSweden());

		return Stream.of(digitalMailRecipients, snailMailRecipients, ineligibleMinorRecipients, undeliverableRecipients)
			.flatMap(Function.identity())
			.toList();
	}

	private Stream<RecipientEntity> createDigitalMailRecipients(final List<String> partyIds) {
		return of(partyIds).orElse(emptyList()).stream()
			.map(entityMapper::toDigitalMailRecipientEntity)
			.filter(Objects::nonNull);
	}

	private Stream<RecipientEntity> createSnailMailRecipients(final List<CitizenExtended> citizens) {
		return ofNullable(citizens).orElse(emptyList()).stream()
			.map(entityMapper::toSnailMailRecipientEntity)
			.filter(Objects::nonNull);
	}

	private Stream<RecipientEntity> createIneligibleMinorRecipients(final List<CitizenExtended> citizens) {
		return ofNullable(citizens).orElse(emptyList()).stream()
			.map(entityMapper::toIneligibleMinorRecipientEntity)
			.filter(Objects::nonNull);
	}

	private Stream<RecipientEntity> createUndeliverableRecipients(final List<CitizenExtended> citizens) {
		return ofNullable(citizens).orElse(emptyList()).stream()
			.map(entityMapper::toUndeliverableRecipientEntity)
			.filter(Objects::nonNull);
	}

	/**
	 * Builds precheck response from categorized partyIds.
	 *
	 * @param  digitalMailPartyIds  partyIds eligible for digital mail
	 * @param  snailMailPartyIds    partyIds eligible for snail mail
	 * @param  ineligiblePartyIds   partyIds ineligible for delivery
	 * @param  unreachableMailboxes unreachable mailboxes with reason information
	 * @return                      precheck response with all recipients
	 */
	private PrecheckResponse createPrecheckResponse(final List<String> digitalMailPartyIds, final List<String> snailMailPartyIds,
		final List<String> ineligiblePartyIds, final List<PrecheckUtil.UnreachableMailbox> unreachableMailboxes) {

		// Create map for partyId -> reason lookups
		final var reasonByPartyId = new LinkedHashMap<String, String>();
		unreachableMailboxes.forEach(unreachable -> reasonByPartyId.put(unreachable.partyId(), unreachable.reason()));

		final var digitalMailRecipients = digitalMailPartyIds.stream()
			.map(partyId -> new PrecheckRecipient(null, partyId, DeliveryMethod.DIGITAL_MAIL, null));

		final var snailMailRecipients = snailMailPartyIds.stream()
			.map(partyId -> new PrecheckRecipient(null, partyId, DeliveryMethod.SNAIL_MAIL, null));

		// Include reason for ineligible recipients
		final var ineligibleRecipients = ineligiblePartyIds.stream()
			.map(partyId -> new PrecheckRecipient(
				null, partyId, DeliveryMethod.DELIVERY_NOT_POSSIBLE, reasonByPartyId.get(partyId)));

		final var recipients = Stream.of(digitalMailRecipients, snailMailRecipients, ineligibleRecipients)
			.flatMap(Function.identity())
			.toList();

		return PrecheckResponse.of(recipients);
	}

	/**
	 * Record for mailbox status results so we know which ones are reachable and not.
	 *
	 * @param reachable             partyIds with reachable digital mailboxes
	 * @param unreachableWithReason unreachable mailboxes with reason information
	 */
	private record MailboxStatus(
		List<String> reachable,
		List<PrecheckUtil.UnreachableMailbox> unreachableWithReason) {
		// Convenience method for precheckLegalIds flow
		public List<String> unreachable() {
			return unreachableWithReason.stream()
				.map(PrecheckUtil.UnreachableMailbox::partyId)
				.toList();
		}
	}
}
