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
import se.sundsvall.postportalservice.integration.db.dao.MessageRepository;
import se.sundsvall.postportalservice.integration.db.dao.RecipientRepository;
import se.sundsvall.postportalservice.integration.db.dao.SigningRepository;
import se.sundsvall.postportalservice.service.util.BlobUtil;

import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.postportalservice.Constants.DECLINED;
import static se.sundsvall.postportalservice.Constants.SIGNED;
import static se.sundsvall.postportalservice.Constants.SIGNERAT;

/**
 * Consumes the normalized signing events relayed by api-service-e-signing. The message id is supplied as a path
 * variable
 * (the {@code customerReference} the provider echoes back), so the signing case is reached directly via
 * {@code message.getSigning()}. Applies a guarded status transition (the terminal {@code SIGNERAT} state is never
 * regressed), updates the acting recipient by party id, and - on completion - stores the signed (merged) document as a
 * new attachment the signing points at. The handler is transactional and idempotent, so redelivered events are safe.
 */
@Service
public class SigningEventService {

	private static final Logger LOG = LoggerFactory.getLogger(SigningEventService.class);

	private final MessageRepository messageRepository;
	private final RecipientRepository recipientRepository;
	private final SigningRepository signingRepository;
	private final BlobUtil blobUtil;

	public SigningEventService(
		final MessageRepository messageRepository,
		final RecipientRepository recipientRepository,
		final SigningRepository signingRepository,
		final BlobUtil blobUtil) {
		this.messageRepository = messageRepository;
		this.recipientRepository = recipientRepository;
		this.signingRepository = signingRepository;
		this.blobUtil = blobUtil;
	}

	@Transactional
	public void handleSigningEvent(final String municipalityId, final String messageId, final SigningEvent event) {
		final var message = messageRepository.findById(messageId).orElse(null);
		final var signing = Optional.ofNullable(message).map(MessageEntity::getSigning).orElse(null);
		if (signing == null) {
			// Ack unknown cases so the provider stops retrying - the create flow persists the signing synchronously, so a
			// message without a signing here is genuinely not an e-signing case.
			LOG.warn("Received signing event for message id {} in municipalityId {} that has no signing case - ignoring",
				sanitizeForLogging(messageId), sanitizeForLogging(municipalityId));
			return;
		}

		applyStatus(signing, event.getStatus());
		Optional.ofNullable(event.getSignatory()).ifPresent(signatory -> updateRecipient(message, signatory));
		Optional.ofNullable(event.getSignedDocument()).ifPresent(document -> storeSignedDocument(message, signing, document));

		signingRepository.save(signing);
	}

	/**
	 * Guarded status transition: {@code SIGNERAT} is terminal (a signed case stays signed), everything else applies the
	 * incoming status (forward progress and reactivation both flow through).
	 */
	void applyStatus(final SigningEntity signing, final String newStatus) {
		if (SIGNERAT.equals(signing.getStatus())) {
			LOG.info("Signing case {} is already {} (terminal); ignoring status {}", signing.getId(), SIGNERAT, newStatus);
			return;
		}
		signing.setStatus(newStatus);
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
		return "DECLINED".equals(action) ? DECLINED : SIGNED;
	}

	/**
	 * Stores the signed document (the merged signed PDF Comfact returns) as a new attachment on the message and points the
	 * signing at it. The original uploaded document(s) are kept.
	 */
	void storeSignedDocument(final MessageEntity message, final SigningEntity signing, final SignedDocument document) {
		final var signedAttachment = AttachmentEntity.create()
			.withFileName(document.getFileName())
			.withContentType(Optional.ofNullable(document.getMimeType()).orElse(APPLICATION_PDF_VALUE))
			.withContent(blobUtil.convertBase64ToBlob(document.getContent()));

		message.getAttachments().add(signedAttachment);
		messageRepository.save(message); // cascade-persists the new attachment and assigns its id
		signing.setAttachment(signedAttachment);
	}
}
