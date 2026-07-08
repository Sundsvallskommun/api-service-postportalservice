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
import se.sundsvall.postportalservice.integration.db.SigningEntity;
import se.sundsvall.postportalservice.integration.db.dao.AttachmentRepository;
import se.sundsvall.postportalservice.integration.db.dao.MessageRepository;
import se.sundsvall.postportalservice.integration.db.dao.RecipientRepository;
import se.sundsvall.postportalservice.integration.db.dao.SigningRepository;
import se.sundsvall.postportalservice.service.util.BlobUtil;

import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.postportalservice.Constants.DECLINED;
import static se.sundsvall.postportalservice.Constants.SIGNED;
import static se.sundsvall.postportalservice.Constants.SIGNERAT;

/**
 * Consumes the normalized signing events relayed by api-service-e-signing. Correlates the event to a
 * {@link SigningEntity} by provider case id, applies a guarded status transition (the terminal {@code SIGNERAT} state
 * is
 * never regressed), updates the acting recipient by party id, and overwrites the stored document with the signed PDF on
 * completion. The whole handler is transactional and idempotent, so redelivered events (Comfact retries the whole
 * chain)
 * are safe.
 */
@Service
public class SigningEventService {

	private static final Logger LOG = LoggerFactory.getLogger(SigningEventService.class);

	private final SigningRepository signingRepository;
	private final MessageRepository messageRepository;
	private final RecipientRepository recipientRepository;
	private final AttachmentRepository attachmentRepository;
	private final BlobUtil blobUtil;

	public SigningEventService(
		final SigningRepository signingRepository,
		final MessageRepository messageRepository,
		final RecipientRepository recipientRepository,
		final AttachmentRepository attachmentRepository,
		final BlobUtil blobUtil) {
		this.signingRepository = signingRepository;
		this.messageRepository = messageRepository;
		this.recipientRepository = recipientRepository;
		this.attachmentRepository = attachmentRepository;
		this.blobUtil = blobUtil;
	}

	@Transactional
	public void handleSigningEvent(final String municipalityId, final SigningEvent event) {
		final var signing = signingRepository.findByProviderCaseId(event.getProviderCaseId()).orElse(null);
		if (signing == null) {
			// Ack unknown cases so the provider stops retrying - the create flow persists the signing synchronously,
			// so a case that cannot be found here will never appear.
			LOG.warn("Received signing event for unknown provider case id {} in municipalityId {} - ignoring",
				sanitizeForLogging(event.getProviderCaseId()), sanitizeForLogging(municipalityId));
			return;
		}

		applyStatus(signing, event.getStatus());
		Optional.ofNullable(event.getSignatory()).ifPresent(signatory -> updateRecipient(signing.getMessageId(), signatory));
		Optional.ofNullable(event.getSignedDocument()).ifPresent(document -> storeSignedDocument(signing, document));

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

	void updateRecipient(final String messageId, final EventSignatory signatory) {
		messageRepository.findById(messageId).ifPresent(message -> message.getRecipients().stream()
			.filter(recipient -> Objects.equals(signatory.getPartyId(), recipient.getPartyId()))
			.findFirst()
			.ifPresent(recipient -> {
				recipient.setStatus(toRecipientStatus(signatory.getAction()));
				recipient.setStatusDetail(signatory.getReason());
				recipientRepository.save(recipient);
			}));
	}

	static String toRecipientStatus(final String action) {
		return "DECLINED".equals(action) ? DECLINED : SIGNED;
	}

	void storeSignedDocument(final SigningEntity signing, final SignedDocument document) {
		Optional.ofNullable(signing.getAttachmentId())
			.flatMap(attachmentRepository::findById)
			.ifPresent(attachment -> {
				attachment.setContent(blobUtil.convertBase64ToBlob(document.getContent()));
				attachmentRepository.save(attachment);
			});
	}
}
