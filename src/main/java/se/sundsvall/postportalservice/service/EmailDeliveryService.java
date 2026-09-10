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
import se.sundsvall.postportalservice.integration.rabbitmq.EmailQueueMessage;
import se.sundsvall.postportalservice.integration.rabbitmq.EmailQueuePublisher;

import static se.sundsvall.postportalservice.integration.rabbitmq.RabbitMapper.toEmailQueueMessage;

/**
 * Sends a single callback e-mail. Behaviour lives in {@link QueuedDeliveryService}.
 * <p>
 * The two paths differ in more than transport here. The REST call reads every attachment out of the database,
 * base64-encodes it and sends the result inline; the queue path uploads each attachment once and sends only its id, so
 * the bytes stop being copied into the message, into messaging's row for it, and onward into the call to email-sender.
 */
@Service
public class EmailDeliveryService extends QueuedDeliveryService<EmailQueueMessage> {

	private final MessagingIntegration messagingIntegration;

	public EmailDeliveryService(
		final MessagingIntegration messagingIntegration,
		final RecipientRepository recipientRepository,
		final AttachmentUploadService attachmentUploadService,
		final Optional<EmailQueuePublisher> emailQueuePublisher) {
		super(recipientRepository, attachmentUploadService, emailQueuePublisher);
		this.messagingIntegration = messagingIntegration;
	}

	public MessageResult deliverEmail(final MessageEntity messageEntity, final RecipientEntity recipientEntity, final Map<String, String> settingsMap) {
		return deliver(messageEntity, recipientEntity, settingsMap);
	}

	@Override
	protected MessageResult sendOverRest(final MessageEntity messageEntity, final RecipientEntity recipientEntity, final Map<String, String> settingsMap) {
		return messagingIntegration.sendCallbackEmail(messageEntity, recipientEntity, settingsMap);
	}

	@Override
	protected EmailQueueMessage toQueueMessage(final MessageEntity messageEntity, final RecipientEntity recipientEntity,
		final Map<String, String> settingsMap, final List<String> objectIds) {
		return toEmailQueueMessage(messageEntity, recipientEntity, settingsMap, objectIds);
	}
}
