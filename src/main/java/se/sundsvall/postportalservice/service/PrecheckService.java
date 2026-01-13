package se.sundsvall.postportalservice.service;

import static java.util.Collections.emptyList;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import generated.se.sundsvall.citizen.CitizenExtended;
import generated.se.sundsvall.citizen.PersonGuidBatch;
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
import se.sundsvall.postportalservice.service.mapper.EntityMapper;
import se.sundsvall.postportalservice.service.util.CitizenCategorizationHelper;
import se.sundsvall.postportalservice.service.util.CitizenCategorizationHelper.SimplifiedCitizen;
import se.sundsvall.postportalservice.service.util.CsvUtil;
import se.sundsvall.postportalservice.service.util.PartyIdMappingHelper;
import se.sundsvall.postportalservice.service.util.PartyIdMappingHelper.PartyIdMapping;
import se.sundsvall.postportalservice.service.util.PrecheckUtil;

@Service
public class PrecheckService {

	private final DigitalRegisteredLetterIntegration digitalRegisteredLetterIntegration;
	private final CitizenIntegration citizenIntegration;
	private final MailboxStatusService mailboxStatusService;
	private final EntityMapper entityMapper;

	public PrecheckService(
		final DigitalRegisteredLetterIntegration digitalRegisteredLetterIntegration,
		final CitizenIntegration citizenIntegration,
		final MailboxStatusService mailboxStatusService,
		final EntityMapper entityMapper) {
		this.digitalRegisteredLetterIntegration = digitalRegisteredLetterIntegration;
		this.citizenIntegration = citizenIntegration;
		this.mailboxStatusService = mailboxStatusService;
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
		final var mailboxStatus = mailboxStatusService.checkMailboxStatus(municipalityId, partyIds);

		// Get citizen details for those without digital mailboxes
		final var citizens = citizenIntegration.getCitizens(municipalityId, mailboxStatus.unreachable());

		var partyIdMapping = getPartyIdMapping(municipalityId, partyIds);

		// Convert CitizenExtended to SimplifiedCitizen and categorize by eligibility
		final var simplifiedCitizens = CitizenCategorizationHelper.fromCitizenExtended(citizens, partyIdMapping);

		var categorizedCitizens = CitizenCategorizationHelper.categorizeCitizens(simplifiedCitizens);

		// Concatenate ineligible minors and not registered in Sweden for precheck response, as both are ineligible for snail
		// mail
		var ineligibleForSnailMail = Stream.concat(
			categorizedCitizens.ineligibleMinors().stream(),
			categorizedCitizens.notRegisteredInSweden().stream())
			.map(SimplifiedCitizen::partyId)
			.toList();

		// Build precheck recipients with reason data
		return createPrecheckResponse(
			mailboxStatus.reachable(),
			categorizedCitizens.eligibleAdults().stream()
				.map(SimplifiedCitizen::partyId)
				.toList(),
			ineligibleForSnailMail,
			mailboxStatus.unreachableWithReason());
	}

	private PartyIdMapping getPartyIdMapping(final String municipalityId, final List<String> partyIds) {
		// Get partyIds and create mapping that we can use for age verification later on
		var partyIdMapping = new PartyIdMapping();

		var citizens = citizenIntegration.getPersonNumbers(municipalityId, partyIds);

		partyIds.forEach(partyId -> {
			// Find corresponding person number for the partyId
			var matchingLegalId = citizens.stream()
				.filter(citizen -> Boolean.TRUE.equals(citizen.getSuccess()))
				.filter(citizen -> citizen.getPersonId() != null)   // Check that we have a personId / UUID
				.filter(citizen -> partyId.equalsIgnoreCase(citizen.getPersonId().toString()))
				.findFirst()
				.map(PersonGuidBatch::getPersonNumber)
				.orElse(null);

			partyIdMapping.addToPartyIdToLegalIdMap(partyId, matchingLegalId);
		});

		return partyIdMapping;
	}

	public List<RecipientEntity> precheckLegalIds(final String municipalityId, final List<String> legalIds) {
		if (legalIds == null || legalIds.isEmpty()) {
			return emptyList();
		}

		// Get partyIds and create mapping that we can use for age verification later on
		final var batches = citizenIntegration.getPartyIds(municipalityId, legalIds);
		final var partyIdMapping = PartyIdMappingHelper.extractPartyIdMapping(batches);

		// Check digital mailbox availability
		final var mailboxStatus = mailboxStatusService.checkMailboxStatus(municipalityId, partyIdMapping.partyIds());

		List<CitizenExtended> citizens = new ArrayList<>();
		// Get citizen details for those without digital mailboxes
		if (!mailboxStatus.unreachable().isEmpty()) {  // No need to call if we have no unreachable partyIds
			citizens = citizenIntegration.getCitizens(municipalityId, mailboxStatus.unreachable());
		}

		// Convert CitizenExtended to SimplifiedCitizen and categorize by eligibility
		final var simplifiedCitizens = CitizenCategorizationHelper.fromCitizenExtended(citizens, partyIdMapping);
		final var categorized = CitizenCategorizationHelper.categorizeCitizens(simplifiedCitizens);

		// Convert categorized citizens to recipient entities
		return createRecipientEntities(mailboxStatus.reachable(), categorized, citizens);
	}

	public List<String> precheckKivra(final String municipalityId, final KivraEligibilityRequest request) {
		return digitalRegisteredLetterIntegration.checkKivraEligibility(municipalityId, request.getPartyIds());
	}

	/**
	 * Builds recipient entities from categorized data.
	 *
	 * @param  digitalMailPartyIds partyIds eligible for digital mail
	 * @param  categorized         categorized citizens
	 * @param  allCitizens         original CitizenExtended list for EntityMapper
	 * @return                     list of recipient entities
	 */
	private List<RecipientEntity> createRecipientEntities(
		final List<String> digitalMailPartyIds,
		final CitizenCategorizationHelper.CategorizedCitizens categorized,
		final List<CitizenExtended> allCitizens) {

		// Create lookup map: partyId -> CitizenExtended
		final var citizenByPartyId = allCitizens.stream()
			.filter(c -> c.getPersonId() != null)
			.collect(Collectors.toMap(
				c -> c.getPersonId().toString(),
				Function.identity(),
				(existing, replacement) -> existing));

		final var digitalMailRecipients = createDigitalMailRecipients(digitalMailPartyIds);
		final var snailMailRecipients = createSnailMailRecipients(categorized.eligibleAdults(), citizenByPartyId);
		final var ineligibleMinorRecipients = createIneligibleMinorRecipients(categorized.ineligibleMinors(), citizenByPartyId);
		final var undeliverableRecipients = createUndeliverableRecipients(categorized.notRegisteredInSweden(), citizenByPartyId);

		return Stream.of(digitalMailRecipients, snailMailRecipients, ineligibleMinorRecipients, undeliverableRecipients)
			.flatMap(Function.identity())
			.toList();
	}

	private Stream<RecipientEntity> createDigitalMailRecipients(final List<String> partyIds) {
		return of(partyIds).orElse(emptyList()).stream()
			.map(entityMapper::toDigitalMailRecipientEntity)
			.filter(Objects::nonNull);
	}

	private Stream<RecipientEntity> createSnailMailRecipients(
		final List<SimplifiedCitizen> citizens,
		final Map<String, CitizenExtended> citizenByPartyId) {

		return ofNullable(citizens).orElse(emptyList()).stream()
			.map(SimplifiedCitizen::partyId)
			.map(citizenByPartyId::get)
			.filter(Objects::nonNull)
			.map(entityMapper::toSnailMailRecipientEntity)
			.filter(Objects::nonNull);
	}

	private Stream<RecipientEntity> createIneligibleMinorRecipients(
		final List<SimplifiedCitizen> citizens,
		final Map<String, CitizenExtended> citizenByPartyId) {

		return ofNullable(citizens).orElse(emptyList()).stream()
			.map(SimplifiedCitizen::partyId)
			.map(citizenByPartyId::get)
			.filter(Objects::nonNull)
			.map(entityMapper::toIneligibleMinorRecipientEntity)
			.filter(Objects::nonNull);
	}

	private Stream<RecipientEntity> createUndeliverableRecipients(
		final List<SimplifiedCitizen> citizens,
		final Map<String, CitizenExtended> citizenByPartyId) {

		return ofNullable(citizens).orElse(emptyList()).stream()
			.map(SimplifiedCitizen::partyId)
			.map(citizenByPartyId::get)
			.filter(Objects::nonNull)
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
}
