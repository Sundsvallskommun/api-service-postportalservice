package se.sundsvall.postportalservice.service;

import generated.se.sundsvall.messaging.MessageResult;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.db.dao.RecipientRepository;
import se.sundsvall.postportalservice.integration.messaging.MessagingIntegration;
import se.sundsvall.postportalservice.integration.objectstore.ObjectStoreIntegration;
import se.sundsvall.postportalservice.integration.rabbitmq.EmailQueuePublisher;
import se.sundsvall.postportalservice.service.util.RecipientId;

import static java.util.Optional.ofNullable;
import static se.sundsvall.postportalservice.Constants.PENDING;
import static se.sundsvall.postportalservice.integration.rabbitmq.RabbitMapper.toEmailQueueMessage;

/**
 * Sends a single callback e-mail, either by publishing it onto the messaging service's queue or, when the queue path is
 * switched off, by calling the messaging REST API as before.
 * <p>
 * The two paths differ in more than transport. The REST call reads every attachment out of the database, base64-encodes
 * it and sends the result inline; the queue path uploads each attachment once and sends only its id, so the bytes stop
 * being copied into the message, into messaging's row for it, and onward into the call to email-sender.
 * <p>
 * They also report differently. The REST call answers with a {@link MessageResult} the caller applies straight away.
 * The queue path answers with nothing: messaging retries a transient failure through its own backoff ladder, so the
 * outcome arrives later on the status queue and is applied by {@link EmailStatusService#handleEmailStatus}. Until then
 * the recipient stays PENDING. A publish the broker does not confirm raises instead, which leaves the recipient FAILED
 * via the caller's catch block.
 */
@Service
public class EmailDeliveryService {

	private final MessagingIntegration messagingIntegration;
	private final RecipientRepository recipientRepository;
	private final ObjectStoreIntegration objectStoreIntegration;
	// Absent unless rabbitmq.enabled=true, in which case e-mail goes over the queue instead of the messaging REST API.
	private final Optional<EmailQueuePublisher> emailQueuePublisher;

	public EmailDeliveryService(
		final MessagingIntegration messagingIntegration,
		final RecipientRepository recipientRepository,
		final ObjectStoreIntegration objectStoreIntegration,
		final Optional<EmailQueuePublisher> emailQueuePublisher) {
		this.messagingIntegration = messagingIntegration;
		this.recipientRepository = recipientRepository;
		this.objectStoreIntegration = objectStoreIntegration;
		this.emailQueuePublisher = emailQueuePublisher;
	}

	public MessageResult deliverEmail(final MessageEntity messageEntity, final RecipientEntity recipientEntity, final Map<String, String> settingsMap) {
		if (emailQueuePublisher.isEmpty()) {
			return messagingIntegration.sendCallbackEmail(messageEntity, recipientEntity, settingsMap);
		}

		RecipientId.init(recipientEntity.getId());

		// Uploaded before the recipient is marked PENDING, so that a store that will not take the attachment fails the
		// send outright rather than leaving a recipient pending on a message that was never published.
		final var objectIds = storeAttachments(messageEntity);

		// PENDING is written before the publish, not after: messaging can be quick enough that the status message
		// arrives while we are still here, and a later write of PENDING would overwrite the outcome it carried.
		recipientEntity.setStatus(PENDING);
		recipientRepository.save(recipientEntity);

		emailQueuePublisher.get().publish(toEmailQueueMessage(messageEntity, recipientEntity, settingsMap, objectIds));
		return null;
	}

	private List<String> storeAttachments(final MessageEntity messageEntity) {
		return ofNullable(messageEntity.getAttachments()).orElse(List.of()).stream()
			.map(objectStoreIntegration::store)
			.toList();
	}
}
