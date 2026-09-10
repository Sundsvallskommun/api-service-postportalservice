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
import se.sundsvall.postportalservice.integration.rabbitmq.SnailMailQueueMessage;
import se.sundsvall.postportalservice.integration.rabbitmq.SnailMailQueuePublisher;

import static se.sundsvall.postportalservice.integration.rabbitmq.RabbitMapper.toSnailMailQueueMessage;

/**
 * Sends physical mail for a single recipient. Behaviour lives in {@link QueuedDeliveryService}.
 * <p>
 * This is the channel where the queue path introduces uploads that were not there before: the REST call sends each
 * attachment inline, so before this existed snail mail stored nothing. It is also the channel where the attachments
 * <em>are</em> the letter, which is why one upload per message rather than per recipient is a precondition for the
 * switch rather than a refinement of it.
 * <p>
 * The batch is not this service's to flush. It sends the message id as the batch id, exactly as it does over REST, and
 * snailmail-sender's own scheduler sweeps batches older than its configured age. There is no completion hook to hang a
 * flush on here, and a flush sent too early posts half a batch as physical mail.
 */
@Service
public class SnailMailDeliveryService extends QueuedDeliveryService<SnailMailQueueMessage> {

	private final MessagingIntegration messagingIntegration;

	public SnailMailDeliveryService(
		final MessagingIntegration messagingIntegration,
		final RecipientRepository recipientRepository,
		final AttachmentUploadService attachmentUploadService,
		final Optional<SnailMailQueuePublisher> snailMailQueuePublisher) {
		super(recipientRepository, attachmentUploadService, snailMailQueuePublisher);
		this.messagingIntegration = messagingIntegration;
	}

	public MessageResult deliverSnailMail(final MessageEntity messageEntity, final RecipientEntity recipientEntity) {
		return deliver(messageEntity, recipientEntity, Map.of());
	}

	@Override
	protected MessageResult sendOverRest(final MessageEntity messageEntity, final RecipientEntity recipientEntity, final Map<String, String> settingsMap) {
		return messagingIntegration.sendSnailMail(messageEntity, recipientEntity);
	}

	@Override
	protected SnailMailQueueMessage toQueueMessage(final MessageEntity messageEntity, final RecipientEntity recipientEntity,
		final Map<String, String> settingsMap, final List<String> objectIds) {
		return toSnailMailQueueMessage(messageEntity, recipientEntity, objectIds);
	}
}
