package se.sundsvall.postportalservice.service;

import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.postportalservice.api.model.EventSignatory;
import se.sundsvall.postportalservice.api.model.SignedDocument;
import se.sundsvall.postportalservice.api.model.SigningEvent;
import se.sundsvall.postportalservice.integration.db.AttachmentEntity;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.SigningEntity;
import se.sundsvall.postportalservice.integration.db.dao.RecipientRepository;
import se.sundsvall.postportalservice.integration.db.dao.SigningRepository;
import se.sundsvall.postportalservice.service.util.BlobUtil;

import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.postportalservice.Constants.CANCELLED;
import static se.sundsvall.postportalservice.Constants.DECLINED;
import static se.sundsvall.postportalservice.Constants.EXPIRED;
import static se.sundsvall.postportalservice.Constants.HALTED;
import static se.sundsvall.postportalservice.Constants.SIGNED;

/**
 * Consumes the normalized signing events relayed by api-service-e-signing. The message id is supplied as a path
 * variable
 * (the {@code customerReference} the provider echoes back), so the signing case is reached via
 * {@code signingRepository.findByMessageId(messageId)}. Applies a guarded status transition (a signed case is never
 * regressed), updates the acting recipient by party id, and - on completion - stores the
 * signed (merged) document as a new attachment the signing points at. The handler is transactional and idempotent, so
 * redelivered events are safe.
 */
@Service
public class SigningEventService {

	private static final Logger LOG = LoggerFactory.getLogger(SigningEventService.class);

	private final RecipientRepository recipientRepository;
	private final SigningRepository signingRepository;
	private final BlobUtil blobUtil;

	public SigningEventService(
		final RecipientRepository recipientRepository,
		final SigningRepository signingRepository,
		final BlobUtil blobUtil) {
		this.recipientRepository = recipientRepository;
		this.signingRepository = signingRepository;
		this.blobUtil = blobUtil;
	}

	@Transactional
	public void handleSigningEvent(final String municipalityId, final String messageId, final SigningEvent event) {
		final var signing = signingRepository.findByMessageId(messageId).orElse(null);
		if (signing == null) {
			// Ack unknown cases so the provider stops retrying - the create flow persists the signing synchronously, so a
			// message without a signing here is genuinely not an e-signing case.
			LOG.warn("Received signing event for message id {} in municipalityId {} that has no signing case - ignoring",
				sanitizeForLogging(messageId), sanitizeForLogging(municipalityId));
			return;
		}
		final var message = signing.getMessage();

		applyStatus(signing, event);
		Optional.ofNullable(event.getSignatory()).ifPresent(signatory -> updateRecipient(message, signatory));
		Optional.ofNullable(event.getSignedDocument()).ifPresent(document -> storeSignedDocument(signing, document));

		signingRepository.save(signing);
	}

	/**
	 * Guarded status transition. {@code SIGNED}, {@code EXPIRED} and {@code CANCELLED} are terminal: a late, redelivered or
	 * out-of-order event never moves the case out of them ({@code CASE_REACTIVATED} may lift an {@code EXPIRED} case).
	 * The provider folds withdrawn, declined and halted cases into {@code FAILED}, so those are derived from the event type
	 * instead of the normalized status ({@code CANCELLED}, {@code DECLINED}, {@code HALTED}); {@code FAILED} is then left
	 * for cases that actually failed, and a withdrawn case does not lose its {@code CANCELLED}.
	 */
	void applyStatus(final SigningEntity signing, final SigningEvent event) {
		final var currentStatus = signing.getStatus();
		final var newStatus = resolveStatus(event);

		if (isTerminal(currentStatus) && !isReactivationOfExpired(currentStatus, event)) {
			LOG.info("Signing case {} is already {} (terminal); ignoring status {}", sanitizeForLogging(signing.getId()), sanitizeForLogging(currentStatus), sanitizeForLogging(newStatus));
			return;
		}
		signing.setStatus(newStatus);
	}

	private static String resolveStatus(final SigningEvent event) {
		return switch (Optional.ofNullable(event.getEventType()).orElse("")) {
			case "CASE_WITHDRAWN" -> CANCELLED;
			case "CASE_EXPIRED" -> EXPIRED;
			case "SIGNATORY_DECLINED" -> DECLINED;
			case "CASE_HALTED" -> HALTED;
			default -> event.getStatus();
		};
	}

	private static boolean isTerminal(final String status) {
		return SIGNED.equals(status) || EXPIRED.equals(status) || CANCELLED.equals(status);
	}

	private static boolean isReactivationOfExpired(final String currentStatus, final SigningEvent event) {
		return EXPIRED.equals(currentStatus) && "CASE_REACTIVATED".equals(event.getEventType());
	}

	void updateRecipient(final MessageEntity message, final EventSignatory signatory) {
		message.getRecipients().stream()
			.filter(recipient -> Objects.equals(signatory.getPartyId(), recipient.getPartyId()))
			.findFirst()
			.ifPresent(recipient -> {
				recipient.setStatus(toRecipientStatus(signatory.getAction()));
				recipient.setStatusDetail(signatory.getReason());
				recipientRepository.save(recipient);
			});
	}

	static String toRecipientStatus(final String action) {
		if (DECLINED.equals(action)) {
			return DECLINED;
		}
		return SIGNED;
	}

	/**
	 * Stores the signed document (the merged signed PDF Comfact returns) on the signing. The original uploaded document(s)
	 * remain as message attachments.
	 */
	void storeSignedDocument(final SigningEntity signing, final SignedDocument document) {
		final var signedAttachment = AttachmentEntity.create()
			.withFileName(document.getFileName())
			.withContentType(Optional.ofNullable(document.getMimeType()).orElse(APPLICATION_PDF_VALUE))
			.withContent(blobUtil.convertBase64ToBlob(document.getContent()));

		signing.setAttachment(signedAttachment);
	}
}
