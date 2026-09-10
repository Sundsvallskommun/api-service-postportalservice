package se.sundsvall.postportalservice.service;

import generated.se.sundsvall.messaging.MessageResult;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.db.dao.RecipientRepository;
import se.sundsvall.postportalservice.integration.rabbitmq.QueueMessage;
import se.sundsvall.postportalservice.integration.rabbitmq.QueuePublisher;
import se.sundsvall.postportalservice.service.util.RecipientId;

import static se.sundsvall.postportalservice.Constants.PENDING;

/**
 * Sends one recipient's message, either by publishing it onto the messaging service's queue or, when the queue path is
 * switched off, by calling the messaging REST API as before.
 * <p>
 * The two paths report their outcome differently, and that difference is the whole reason this sits between
 * {@link MessageService} and the transports. The REST call answers with a {@link MessageResult} the caller applies
 * straight away. The queue path answers with {@code null}: messaging retries a transient failure through its own
 * backoff ladder, so the outcome arrives later on the status queue and is applied by
 * {@link RecipientStatusService#handleStatus}. Until then the recipient stays PENDING. A publish the broker does not
 * confirm raises instead, which leaves the recipient FAILED via the caller's catch block.
 * <p>
 * The ordering inside {@link #deliver} is the part worth keeping identical across channels, which is why it lives here
 * rather than being copied four times.
 *
 * @param <T> this channel's queue payload
 */
public abstract class QueuedDeliveryService<T extends QueueMessage> {

	private final RecipientRepository recipientRepository;
	private final AttachmentUploadService attachmentUploadService;
	// Absent unless rabbitmq.enabled=true, in which case this channel goes over the queue instead of the REST API.
	private final Optional<? extends QueuePublisher<T>> queuePublisher;

	protected QueuedDeliveryService(final RecipientRepository recipientRepository, final AttachmentUploadService attachmentUploadService,
		final Optional<? extends QueuePublisher<T>> queuePublisher) {
		this.recipientRepository = recipientRepository;
		this.attachmentUploadService = attachmentUploadService;
		this.queuePublisher = queuePublisher;
	}

	/**
	 * The path this channel took before the queue existed, and the one it falls back to when the flag is off.
	 */
	protected abstract MessageResult sendOverRest(MessageEntity messageEntity, RecipientEntity recipientEntity, Map<String, String> settingsMap);

	protected abstract T toQueueMessage(MessageEntity messageEntity, RecipientEntity recipientEntity, Map<String, String> settingsMap,
		List<String> objectIds);

	/**
	 * Whether this channel's queue payload names attachments. Overridden to {@code false} by SMS, whose message is its
	 * body alone - uploading a letter's attachments for it would fill the object store with objects nothing references.
	 */
	protected boolean carriesAttachments() {
		return true;
	}

	public final MessageResult deliver(final MessageEntity messageEntity, final RecipientEntity recipientEntity, final Map<String, String> settingsMap) {
		if (queuePublisher.isEmpty()) {
			return sendOverRest(messageEntity, recipientEntity, settingsMap);
		}

		RecipientId.init(recipientEntity.getId());

		// Uploaded before the recipient is marked PENDING, so that a store that will not take the attachment fails the
		// send outright rather than leaving a recipient pending on a message that was never published. The upload
		// itself happens once per message, not once per recipient - see AttachmentUploadService.
		final var objectIds = storeAttachments(messageEntity);

		// PENDING is written before the publish, not after: messaging can be quick enough that the status message
		// arrives while we are still here, and a later write of PENDING would overwrite the outcome it carried.
		recipientEntity.setStatus(PENDING);
		recipientRepository.save(recipientEntity);

		queuePublisher.get().publish(toQueueMessage(messageEntity, recipientEntity, settingsMap, objectIds));
		return null;
	}

	private List<String> storeAttachments(final MessageEntity messageEntity) {
		if (!carriesAttachments()) {
			return List.of();
		}
		return attachmentUploadService.storeOnce(messageEntity);
	}
}
