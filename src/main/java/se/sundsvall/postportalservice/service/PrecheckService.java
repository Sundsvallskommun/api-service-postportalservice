package se.sundsvall.postportalservice.service;

import static java.util.Collections.emptyList;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static se.sundsvall.postportalservice.Constants.INELIGIBLE_MINOR;

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
import se.sundsvall.postportalservice.service.MailboxStatusService.MailboxStatus;
import se.sundsvall.postportalservice.service.mapper.EntityMapper;
import se.sundsvall.postportalservice.service.util.CitizenCategorizationHelper;
import se.sundsvall.postportalservice.service.util.CitizenCategorizationHelper.CategorizedCitizens;
import se.sundsvall.postportalservice.service.util.CitizenCategorizationHelper.SimplifiedCitizen;
import se.sundsvall.postportalservice.service.util.CsvUtil;
import se.sundsvall.postportalservice.service.util.PartyIdMappingHelper;
import se.sundsvall.postportalservice.service.util.PartyIdMappingHelper.PartyIdMapping;

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
		final var occurrenceMap = CsvUtil.validateCSV(csvFile);

		// Filter the map to include only entries with more than one occurrence
		final var duplicateEntriesMap = occurrenceMap.entrySet().stream()
			.filter(entry -> entry.getValue() > 1)
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		return new PrecheckCsvResponse(duplicateEntriesMap);
	}

	public PrecheckResponse precheckPartyIds(final String municipalityId, final List<String> partyIds) {
		// Check if we can send using digital mail
		final var mailboxStatus = mailboxStatusService.checkMailboxStatus(municipalityId, partyIds);

		// Get citizen details for those without digital mailboxes
		final var citizens = citizenIntegration.getCitizens(municipalityId, mailboxStatus.unreachable());

		final var partyIdMapping = getPartyIdMapping(municipalityId, partyIds);

		// Convert CitizenExtended to SimplifiedCitizen and categorize by eligibility
		final var simplifiedCitizens = CitizenCategorizationHelper.fromCitizenExtended(citizens, partyIdMapping);

		// Categorize and perform age verification
		final var categorizedCitizens = CitizenCategorizationHelper.categorizeCitizens(simplifiedCitizens);

		// Build precheck recipients with reason data
		return createPrecheckResponse(mailboxStatus, categorizedCitizens);
	}

	private PartyIdMapping getPartyIdMapping(final String municipalityId, final List<String> partyIds) {
		// Get partyIds and create mapping that we can use for age verification later on
		final var partyIdMapping = new PartyIdMapping();

		final var citizens = citizenIntegration.getPersonNumbers(municipalityId, partyIds);

		partyIds.forEach(partyId -> {
			// Find corresponding person number for the partyId
			final var matchingLegalId = citizens.stream()
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
		final CategorizedCitizens categorized,
		final List<CitizenExtended> allCitizens) {

		// Create lookup map: partyId -> CitizenExtended
		final var citizenByPartyId = ofNullable(allCitizens)
			.orElse(emptyList()).stream()
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
	 * @param  mailboxStatus       mailbox status containing reachable and unreachable partyIds for digital mail
	 * @param  categorizedCitizens categorized citizens by eligibility
	 * @return                     precheck response with recipients and delivery methods
	 */
	private PrecheckResponse createPrecheckResponse(MailboxStatus mailboxStatus, final CategorizedCitizens categorizedCitizens) {
		// Create map for partyId -> reason lookups
		final var reasonByPartyId = new LinkedHashMap<String, String>();
		mailboxStatus.unreachableWithReason().forEach(unreachable -> reasonByPartyId.put(unreachable.partyId(), unreachable.reason()));

		// Map reachable by digital-mail
		final var digitalMailRecipients = mailboxStatus.reachable().stream()
			.map(partyId -> new PrecheckRecipient(null, partyId, DeliveryMethod.DIGITAL_MAIL, null));

		// Map reachable by snail-mail
		final var snailMailRecipients = categorizedCitizens.eligibleAdults().stream()
			.map(simplifiedCitizen -> new PrecheckRecipient(null, simplifiedCitizen.partyId(), DeliveryMethod.SNAIL_MAIL, null));

		// Map ineligible recipients with reason
		final var ineligibleRecipients = categorizedCitizens.notRegisteredInSweden().stream()
			.map(simplifiedCitizen -> new PrecheckRecipient(
				null, simplifiedCitizen.partyId(), DeliveryMethod.DELIVERY_NOT_POSSIBLE, reasonByPartyId.get(simplifiedCitizen.partyId())));

		// Map ineligible minors with fixed reason
		final var ineligibleMinorRecipients = categorizedCitizens.ineligibleMinors().stream()
			.map(simplifiedCitizen -> new PrecheckRecipient(
				null, simplifiedCitizen.partyId(), DeliveryMethod.DELIVERY_NOT_POSSIBLE, INELIGIBLE_MINOR));

		final var recipients = Stream.of(digitalMailRecipients, snailMailRecipients, ineligibleRecipients, ineligibleMinorRecipients)
			.flatMap(Function.identity())
			.toList();

		return PrecheckResponse.of(recipients);
	}
}
