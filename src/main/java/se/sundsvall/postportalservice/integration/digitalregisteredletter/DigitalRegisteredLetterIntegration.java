package se.sundsvall.postportalservice.integration.digitalregisteredletter;

import static java.util.Collections.emptyList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.postportalservice.Constants.FAILED;
import static se.sundsvall.postportalservice.service.util.IdentifierUtil.getIdentifierHeaderValue;

import generated.se.sundsvall.digitalregisteredletter.LetterStatus;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.zalando.problem.Problem;
import se.sundsvall.postportalservice.api.model.SigningInformation;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.service.util.RecipientId;

@Component
public class DigitalRegisteredLetterIntegration {

	private static final Logger LOGGER = LoggerFactory.getLogger(DigitalRegisteredLetterIntegration.class);

	private final DigitalRegisteredLetterClient client;
	private final DigitalRegisteredLetterMapper mapper;

	public DigitalRegisteredLetterIntegration(final DigitalRegisteredLetterClient client, final DigitalRegisteredLetterMapper mapper) {
		this.client = client;
		this.mapper = mapper;
	}

	/**
	 * Takes a list of partyIds and checks their Kivra eligibility, returning a list of eligible partyIds.
	 *
	 * @param  municipalityId the municipality id
	 * @param  partyIds       the party ids to check
	 * @return                a list of eligible partyIds
	 */
	public List<String> checkKivraEligibility(final String municipalityId, final List<String> partyIds) {
		final var request = mapper.toEligibilityRequest(partyIds);
		return client.checkKivraEligibility(municipalityId, request);
	}

	public SigningInformation getSigningInformation(final String municipalityId, final String letterId) {
		final var info = client.getSigningInfo(municipalityId, letterId);
		return mapper.toSigningInformation(info);
	}

	public List<LetterStatus> getLetterStatuses(final String municipalityId, final List<String> letterIds) {
		if (isEmpty(letterIds)) {
			return emptyList();
		}

		final var request = mapper.toLetterStatusRequest(letterIds);
		return client.getLetterStatuses(municipalityId, request);
	}

	public void sendLetter(final MessageEntity messageEntity, final RecipientEntity recipientEntity) {
		RecipientId.init(recipientEntity.getId());
		LOGGER.info("Sending digital registered letter for recipientId: {} in municipalityId: {}", sanitizeForLogging(recipientEntity.getId()), sanitizeForLogging(messageEntity.getMunicipalityId()));
		try {
			final var request = mapper.toLetterRequest(messageEntity, recipientEntity);
			final var multipartFiles = mapper.toMultipartFiles(messageEntity.getAttachments());
			final var letter = client.sendLetter(getIdentifierHeaderValue(messageEntity.getUser().getUsername()),
				messageEntity.getMunicipalityId(),
				request,
				multipartFiles);
			recipientEntity.setExternalId(letter.getId());
			recipientEntity.setStatus(letter.getStatus());
			LOGGER.info("Successfully sent digital registered letter for recipientId: {}, externalId: {}, status: {}", sanitizeForLogging(recipientEntity.getId()), sanitizeForLogging(letter.getId()), sanitizeForLogging(letter.getStatus()));
		} catch (final Exception e) {
			LOGGER.error("Failed to send digital registered letter for recipientId: {} in messageId: {}", sanitizeForLogging(recipientEntity.getId()), sanitizeForLogging(messageEntity.getId()), e);
			recipientEntity.setStatus(FAILED);
			recipientEntity.setStatusDetail(e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

	public ResponseEntity<StreamingResponseBody> getLetterReceipt(final String municipalityId, final String letterId) {
		final var feignResponse = client.getLetterReceipt(municipalityId, letterId);
		final var headers = feignResponse.headers();

		final var newHeaders = new HttpHeaders();
		Optional.ofNullable(headers.get("Content-Type"))
			.flatMap(values -> values.stream().findFirst())
			.ifPresentOrElse(value1 -> newHeaders.set("Content-Type", value1),
				() -> {
					throw Problem.valueOf(INTERNAL_SERVER_ERROR, "Missing Content-Type header in letter receipt response");
				});

		Optional.ofNullable(headers.get("Content-Disposition"))
			.flatMap(values -> values.stream().findFirst())
			.ifPresentOrElse(value1 -> newHeaders.set("Content-Disposition", value1),
				() -> {
					throw Problem.valueOf(INTERNAL_SERVER_ERROR, "Missing Content-Disposition header in letter receipt response");
				});

		final StreamingResponseBody streamingResponseBody = outputStream -> {
			try (final InputStream inputStream = feignResponse.body().asInputStream()) {
				inputStream.transferTo(outputStream);
			} catch (final IOException e) {
				throw Problem.valueOf(INTERNAL_SERVER_ERROR, "Could not stream letter receipt: " + e.getMessage());
			}
		};

		return ResponseEntity
			.status(feignResponse.status())
			.headers(newHeaders)
			.body(streamingResponseBody);
	}

}
