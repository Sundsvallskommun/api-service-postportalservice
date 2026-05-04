package se.sundsvall.postportalservice.service;

import generated.se.sundsvall.citizen.CitizenExtended;
import generated.se.sundsvall.legalentity.LegalEntity2;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.postportalservice.api.model.KivraEligibilityRequest;
import se.sundsvall.postportalservice.api.model.PrecheckCsvResponse;
import se.sundsvall.postportalservice.api.model.PrecheckResponse;
import se.sundsvall.postportalservice.api.model.PrecheckResponse.DeliveryMethod;
import se.sundsvall.postportalservice.api.model.PrecheckResponse.PrecheckRecipient;
import se.sundsvall.postportalservice.integration.citizen.CitizenIntegration;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.db.converter.PartyType;
import se.sundsvall.postportalservice.integration.digitalregisteredletter.DigitalRegisteredLetterIntegration;
import se.sundsvall.postportalservice.integration.legalentity.LegalEntityIntegration;
import se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration;
import se.sundsvall.postportalservice.integration.party.PartyIntegration;
import se.sundsvall.postportalservice.service.mapper.EntityMapper;
import se.sundsvall.postportalservice.service.util.CitizenCategorizationHelper;
import se.sundsvall.postportalservice.service.util.CitizenCategorizationHelper.CategorizedCitizens;
import se.sundsvall.postportalservice.service.util.CitizenCategorizationHelper.SimplifiedCitizen;
import se.sundsvall.postportalservice.service.util.CsvUtil;
import se.sundsvall.postportalservice.service.util.PartyIdMappingHelper;
import se.sundsvall.postportalservice.service.util.PartyIdMappingHelper.PartyIdMapping;

import static java.util.Collections.emptyList;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static se.sundsvall.postportalservice.Constants.INELIGIBLE_MINOR;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.ORGANIZATION_NUMBER;

@Service
public class PrecheckService {

	private final DigitalRegisteredLetterIntegration digitalRegisteredLetterIntegration;
	private final CitizenIntegration citizenIntegration;
	private final MailboxStatusService mailboxStatusService;
	private final EntityMapper entityMapper;
	private final PartyIntegration partyIntegration;
	private final MessagingSettingsIntegration messagingSettingsIntegration;
	private final LegalEntityIntegration legalEntityIntegration;

	public PrecheckService(
		final DigitalRegisteredLetterIntegration digitalRegisteredLetterIntegration,
		final CitizenIntegration citizenIntegration,
		final MailboxStatusService mailboxStatusService,
		final EntityMapper entityMapper,
		final PartyIntegration partyIntegration,
		final MessagingSettingsIntegration messagingSettingsIntegration,
		final LegalEntityIntegration legalEntityIntegration) {
		this.digitalRegisteredLetterIntegration = digitalRegisteredLetterIntegration;
		this.citizenIntegration = citizenIntegration;
		this.mailboxStatusService = mailboxStatusService;
		this.entityMapper = entityMapper;
		this.partyIntegration = partyIntegration;
		this.messagingSettingsIntegration = messagingSettingsIntegration;
		this.legalEntityIntegration = legalEntityIntegration;
	}

	public PrecheckCsvResponse precheckSmsCsv(final MultipartFile csvFile) {
		final var result = CsvUtil.validateSmsCsv(csvFile);

		if (result.validEntries().isEmpty()) {
			throw Problem.valueOf(BAD_REQUEST, "No valid phone numbers found in the provided CSV file");
		}

		final var duplicateEntriesMap = result.validEntries().entrySet().stream()
			.filter(entry -> entry.getValue() > 1)
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		return new PrecheckCsvResponse(duplicateEntriesMap, result.invalidEntries());
	}

	public PrecheckCsvResponse precheckLetterCsv(final String municipalityId, final MultipartFile csvFile) {
		final var parsed = CsvUtil.parseLetterCsv(csvFile);

		final var allOccurrences = new LinkedHashMap<String, Integer>();
		allOccurrences.putAll(parsed.privateIds());
		allOccurrences.putAll(parsed.enterpriseIds());

		final var legalIds = allOccurrences.keySet();

		// Filter the map to include only entries with more than one occurrence
		final var duplicateEntriesMap = allOccurrences.entrySet().stream()
			.filter(entry -> entry.getValue() > 1)
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		final var privateMap = partyIntegration.getPartyIds(municipalityId, new ArrayList<>(parsed.privateIds().keySet()));
		final var enterpriseMap = partyIntegration.getEnterprisePartyIds(municipalityId, new ArrayList<>(parsed.enterpriseIds().keySet()));

		if (privateMap.isEmpty() && enterpriseMap.isEmpty()) {
			throw Problem.valueOf(BAD_REQUEST, "No valid partyIds found for the provided legal IDs");
		}

		final var legalIdsWithoutPartyId = legalIds.stream()
			.filter(legalId -> privateMap.get(legalId) == null && enterpriseMap.get(legalId) == null)
			.collect(Collectors.toSet());

		return new PrecheckCsvResponse(duplicateEntriesMap, legalIdsWithoutPartyId);
	}

	public PrecheckResponse precheckPartyIds(final String municipalityId, final List<String> partyIds) {
		// Resolve type per partyId; unresolved partyIds default to PRIVATE (preserves prior behavior).
		final var partyTypes = partyIntegration.getPartyTypes(municipalityId, partyIds);
		final var enterprisePartyIds = partyIds.stream()
			.filter(partyId -> partyTypes.get(partyId) == PartyType.ENTERPRISE)
			.toList();
		final var privatePartyIds = partyIds.stream()
			.filter(partyId -> !enterprisePartyIds.contains(partyId))
			.toList();

		final var enterpriseRecipients = enterprisePartyIds.isEmpty()
			? Stream.<PrecheckRecipient>empty()
			: createEnterprisePrecheckResponse(municipalityId, enterprisePartyIds).precheckRecipients().stream();

		final var privateRecipients = privatePartyIds.isEmpty()
			? Stream.<PrecheckRecipient>empty()
			: createPrivatePrecheckResponse(municipalityId, privatePartyIds).precheckRecipients().stream();

		return PrecheckResponse.of(Stream.concat(privateRecipients, enterpriseRecipients).toList());
	}

	private PrecheckResponse createPrivatePrecheckResponse(final String municipalityId, final List<String> partyIds) {
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

	private PrecheckResponse createEnterprisePrecheckResponse(final String municipalityId, final List<String> partyIds) {
		final var mailboxStatus = mailboxStatusService.checkMailboxStatus(municipalityId, partyIds);
		final var legalEntities = legalEntityIntegration.getLegalEntities(municipalityId, mailboxStatus.unreachable());

		final var reasonByPartyId = new LinkedHashMap<String, String>();
		mailboxStatus.unreachableWithReason().forEach(unreachable -> reasonByPartyId.put(unreachable.partyId(), unreachable.reason()));

		final var digital = mailboxStatus.reachable().stream()
			.map(partyId -> new PrecheckRecipient(null, partyId, DeliveryMethod.DIGITAL_MAIL, null));

		final var fallback = mailboxStatus.unreachable().stream()
			.map(partyId -> {
				final var hasAddress = Optional.ofNullable(legalEntities.get(partyId))
					.map(LegalEntity2::getAddress)
					.isPresent();
				if (hasAddress) {
					return new PrecheckRecipient(null, partyId, DeliveryMethod.SNAIL_MAIL, null);
				}
				return new PrecheckRecipient(null, partyId, DeliveryMethod.DELIVERY_NOT_POSSIBLE, reasonByPartyId.get(partyId));
			});

		return PrecheckResponse.of(Stream.concat(digital, fallback).toList());
	}

	private PartyIdMapping getPartyIdMapping(final String municipalityId, final List<String> partyIds) {
		// Get legalIds for partyIds and create mapping that we can use for age verification later on
		final var partyIdMapping = new PartyIdMapping();

		final var partyIdToLegalIdMap = partyIntegration.getLegalIds(municipalityId, partyIds);

		partyIds.forEach(partyId -> {
			final var matchingLegalId = partyIdToLegalIdMap.get(partyId);
			partyIdMapping.addToPartyIdToLegalIdMap(partyId, matchingLegalId);
		});

		return partyIdMapping;
	}

	public List<RecipientEntity> precheckLegalIds(final String municipalityId, final List<String> privateIds, final List<String> enterpriseIds) {
		final var privateRecipients = precheckPrivateLegalIds(municipalityId, privateIds);
		final var enterpriseRecipients = precheckEnterpriseLegalIds(municipalityId, enterpriseIds);
		return Stream.concat(privateRecipients.stream(), enterpriseRecipients.stream()).toList();
	}

	private List<RecipientEntity> precheckPrivateLegalIds(final String municipalityId, final List<String> legalIds) {
		if (legalIds == null || legalIds.isEmpty()) {
			return emptyList();
		}

		// Get partyIds and create mapping that we can use for age verification later on
		final var legalIdToPartyIdMap = partyIntegration.getPartyIds(municipalityId, legalIds);
		final var partyIdMapping = PartyIdMappingHelper.extractPartyIdMappingFromMap(legalIdToPartyIdMap);

		// Check digital mailbox availability
		final var mailboxStatus = mailboxStatusService.checkMailboxStatus(municipalityId, partyIdMapping.partyIds());

		// Get citizen details for all partyIds
		final var citizens = citizenIntegration.getCitizens(municipalityId, partyIdMapping.partyIds());

		// Convert CitizenExtended to SimplifiedCitizen and categorize by eligibility, filtering only unreachable ones as the
		// others are handled directly
		final var simplifiedCitizens = CitizenCategorizationHelper.fromCitizenExtended(citizens.stream()
			.filter(citizenExtended -> mailboxStatus.unreachable().contains(String.valueOf(citizenExtended.getPersonId())))
			.toList(), partyIdMapping);

		final var categorized = CitizenCategorizationHelper.categorizeCitizens(simplifiedCitizens);

		// Convert categorized citizens to recipient entities
		return createRecipientEntities(mailboxStatus.reachable(), categorized, citizens);
	}

	private List<RecipientEntity> precheckEnterpriseLegalIds(final String municipalityId, final List<String> legalIds) {
		if (legalIds == null || legalIds.isEmpty()) {
			return emptyList();
		}

		final var legalIdToPartyIdMap = partyIntegration.getEnterprisePartyIds(municipalityId, legalIds);
		final var partyIds = new ArrayList<>(legalIdToPartyIdMap.values());

		if (partyIds.isEmpty()) {
			return emptyList();
		}

		final var mailboxStatus = mailboxStatusService.checkMailboxStatus(municipalityId, partyIds);
		final var legalEntities = legalEntityIntegration.getLegalEntities(municipalityId, mailboxStatus.unreachable());

		final var reasonByPartyId = new LinkedHashMap<String, String>();
		mailboxStatus.unreachableWithReason().forEach(unreachable -> reasonByPartyId.put(unreachable.partyId(), unreachable.reason()));

		final var digital = mailboxStatus.reachable().stream()
			.map(entityMapper::toEnterpriseDigitalMailRecipientEntity)
			.filter(Objects::nonNull);

		final var fallback = mailboxStatus.unreachable().stream()
			.map(partyId -> {
				final var legalEntity = legalEntities.get(partyId);
				final var snail = entityMapper.toEnterpriseSnailMailRecipientEntity(partyId, legalEntity);
				if (snail != null) {
					return snail;
				}
				return entityMapper.toEnterpriseUndeliverableRecipientEntity(partyId, reasonByPartyId.get(partyId));
			})
			.filter(Objects::nonNull);

		return Stream.concat(digital, fallback).toList();
	}

	public List<String> precheckKivra(final String municipalityId, final KivraEligibilityRequest request) {
		final var settingsMap = messagingSettingsIntegration.getMessagingSettingsForUser(municipalityId);
		final var organizationNumber = settingsMap.get(ORGANIZATION_NUMBER);

		return digitalRegisteredLetterIntegration.checkKivraEligibility(municipalityId, organizationNumber, request.getPartyIds());
	}

	/**
	 * Builds recipient entities from categorized data.
	 *
	 * @param  digitalMailPartyIds partyIds eligible for digital mail
	 * @param  categorized         categorized citizens
	 * @param  allCitizens         all retrieved citizen details
	 * @return                     list of recipient entities
	 */
	private List<RecipientEntity> createRecipientEntities(final List<String> digitalMailPartyIds, final CategorizedCitizens categorized, final List<CitizenExtended> allCitizens) {
		// Create lookup map: partyId -> CitizenExtended
		final var citizenByPartyId = ofNullable(allCitizens).orElse(emptyList()).stream()
			.filter(citizenExtended -> citizenExtended.getPersonId() != null)
			.collect(Collectors.toMap(
				citizenExtended -> citizenExtended.getPersonId().toString(),
				Function.identity(),
				(existing, _) -> existing));

		final var digitalMailRecipients = createDigitalMailRecipients(digitalMailPartyIds, citizenByPartyId);
		final var snailMailRecipients = createSnailMailRecipients(categorized.eligibleAdults(), citizenByPartyId);
		final var ineligibleMinorRecipients = createIneligibleMinorRecipients(categorized.ineligibleMinors(), citizenByPartyId);
		final var undeliverableRecipients = createUndeliverableRecipients(categorized.notRegisteredInSweden(), citizenByPartyId);

		return Stream.of(digitalMailRecipients, snailMailRecipients, ineligibleMinorRecipients, undeliverableRecipients)
			.flatMap(Function.identity())
			.toList();
	}

	private Stream<RecipientEntity> createDigitalMailRecipients(final List<String> partyIds, final Map<String, CitizenExtended> citizenByPartyId) {
		return of(partyIds).orElse(emptyList()).stream()
			.map(partyId -> entityMapper.toDigitalMailRecipientEntity(partyId, citizenByPartyId.get(partyId)))
			.filter(Objects::nonNull);
	}

	private Stream<RecipientEntity> createSnailMailRecipients(final List<SimplifiedCitizen> citizens, final Map<String, CitizenExtended> citizenByPartyId) {
		return ofNullable(citizens).orElse(emptyList()).stream()
			.map(SimplifiedCitizen::partyId)
			.map(citizenByPartyId::get)
			.filter(Objects::nonNull)
			.map(entityMapper::toSnailMailRecipientEntity)
			.filter(Objects::nonNull);
	}

	private Stream<RecipientEntity> createIneligibleMinorRecipients(final List<SimplifiedCitizen> citizens, final Map<String, CitizenExtended> citizenByPartyId) {
		return ofNullable(citizens).orElse(emptyList()).stream()
			.map(SimplifiedCitizen::partyId)
			.map(citizenByPartyId::get)
			.filter(Objects::nonNull)
			.map(entityMapper::toIneligibleMinorRecipientEntity)
			.filter(Objects::nonNull);
	}

	private Stream<RecipientEntity> createUndeliverableRecipients(final List<SimplifiedCitizen> citizens, final Map<String, CitizenExtended> citizenByPartyId) {
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
	private PrecheckResponse createPrecheckResponse(final MailboxStatusService.MailboxStatus mailboxStatus, final CategorizedCitizens categorizedCitizens) {
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
