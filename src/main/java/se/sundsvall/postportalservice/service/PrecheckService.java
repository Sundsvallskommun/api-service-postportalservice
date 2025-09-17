package se.sundsvall.postportalservice.service;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.zalando.problem.Problem;
import se.sundsvall.postportalservice.api.model.PrecheckResponse;
import se.sundsvall.postportalservice.api.model.PrecheckResponse.DeliveryMethod;
import se.sundsvall.postportalservice.api.model.PrecheckResponse.RecipientPrecheck;
import se.sundsvall.postportalservice.integration.citizen.CitizenIntegration;
import se.sundsvall.postportalservice.integration.messaging.MessagingIntegration;
import se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration;
import se.sundsvall.postportalservice.service.mapper.PrecheckMapper;
import se.sundsvall.postportalservice.service.util.PrecheckUtil;

@Service
public class PrecheckService {

	private static final Logger LOGGER = LoggerFactory.getLogger(PrecheckService.class);

	public static final String CSV_DELIMITER = ";";
	public static final int PERSONAL_IDENTITY_NUMBER_INDEX = 0;

	public static final String FAILURE_REASON_PARTY_ID_NOT_FOUND = "Party ID not found.";
	public static final String FAILURE_REASON_NO_ELIGIBLE_DELIVERY_METHOD = "No eligible delivery method.";
	public static final String FAILURE_REASON_UNKNOWN_ERROR = "Unknown error.";

	private final CitizenIntegration citizenIntegration;
	private final MessagingSettingsIntegration messagingSettingsIntegration;
	private final MessagingIntegration messagingIntegration;
	private final PrecheckMapper precheckMapper;

	public PrecheckService(final CitizenIntegration citizenIntegration, final MessagingSettingsIntegration messagingSettingsIntegration, final MessagingIntegration messagingIntegration, final PrecheckMapper precheckMapper) {
		this.citizenIntegration = citizenIntegration;
		this.messagingSettingsIntegration = messagingSettingsIntegration;
		this.messagingIntegration = messagingIntegration;
		this.precheckMapper = precheckMapper;
	}

	public record PrecheckEntry(String personalIdentityNumber) {}

	public List<PrecheckEntry> parseCsv(MultipartFile file) {
		ofNullable(file)
			.filter(f -> !f.isEmpty())
			.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, "Uploaded file is empty"));

		try (var reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
			return reader.lines()
				.skip(1) // ignore header
				.map(line -> line.split(CSV_DELIMITER, 2))
				.map(columns -> columns[PERSONAL_IDENTITY_NUMBER_INDEX].trim())
				.filter(personalIdentityNumber -> !personalIdentityNumber.isBlank())
				.map(PrecheckUtil::normalizePersonalIdentityNumber)
				.map(PrecheckEntry::new)
				.toList();
		} catch (IOException e) {
			LOGGER.error("Failed to parse CSV", e);
			throw Problem.valueOf(BAD_REQUEST, "Failed to parse CSV");
		}
	}

	public PrecheckResponse precheck(String municipalityId, String departmentId, List<PrecheckEntry> entries) {
		final var personIds = precheckMapper.toPersonIds(entries);

		if (personIds.isEmpty()) {
			return PrecheckResponse.of(emptyList());
		}

		final var batches = citizenIntegration.getPartyIds(municipalityId, personIds);
		final var failureByPersonId = precheckMapper.toFailureByPersonId(batches);
		final var okRows = PrecheckUtil.filterSuccessfulPersonGuidBatches(batches);
		final var personIdToPartyId = precheckMapper.mapPersonIdToPartyId(okRows);
		final var partyIds = PrecheckUtil.filterNonNull(personIdToPartyId);

		if (partyIds.isEmpty()) {
			final var recipients = precheckMapper.toRecipientsWithoutPartyIds(personIds, failureByPersonId);

			return PrecheckResponse.of(recipients);
		}

		final var orgNumber = messagingSettingsIntegration.getOrganizationNumber(municipalityId, departmentId)
			.orElseThrow(() -> Problem.valueOf(INTERNAL_SERVER_ERROR, "Organization number not found."));

		final var mailboxes = messagingIntegration.precheckMailboxes(municipalityId, orgNumber, partyIds);
		final var reachable = PrecheckUtil.filterReachableMailboxes(mailboxes);
		final var unreachable = PrecheckUtil.filterUnreachableMailboxes(mailboxes);
		final var citizens = citizenIntegration.getCitizens(municipalityId, unreachable);
		final var snailMail = precheckMapper.toSnailMailEligiblePartyIds(citizens, citizenIntegration::isRegisteredInSweden);

		final var recipients = personIds.stream()
			.map(personId -> {
				final var failure = failureByPersonId.get(personId);

				if (failure != null) {
					return new RecipientPrecheck(personId, null, DeliveryMethod.DELIVERY_NOT_POSSIBLE, failure);
				}

				final var partyId = personIdToPartyId.get(personId);
				final var method = PrecheckUtil.getDeliveryMethod(partyId, reachable, snailMail);

				String reason = null;

				if (partyId == null) {
					reason = FAILURE_REASON_PARTY_ID_NOT_FOUND;
				} else if (method == DeliveryMethod.DELIVERY_NOT_POSSIBLE) {
					reason = FAILURE_REASON_NO_ELIGIBLE_DELIVERY_METHOD;
				}

				return new RecipientPrecheck(personId, partyId, method, reason);
			})
			.toList();

		return PrecheckResponse.of(recipients);
	}
}
