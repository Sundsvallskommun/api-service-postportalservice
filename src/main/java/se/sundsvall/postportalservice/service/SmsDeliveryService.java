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
import se.sundsvall.postportalservice.integration.rabbitmq.SmsQueueMessage;
import se.sundsvall.postportalservice.integration.rabbitmq.SmsQueuePublisher;

import static se.sundsvall.postportalservice.integration.rabbitmq.RabbitMapper.toSmsQueueMessage;

/**
 * Sends a single SMS. Behaviour lives in {@link QueuedDeliveryService}.
 */
@Service
public class SmsDeliveryService extends QueuedDeliveryService<SmsQueueMessage> {

	private final MessagingIntegration messagingIntegration;

	public SmsDeliveryService(
		final MessagingIntegration messagingIntegration,
		final RecipientRepository recipientRepository,
		final AttachmentUploadService attachmentUploadService,
		final Optional<SmsQueuePublisher> smsQueuePublisher) {
		super(recipientRepository, attachmentUploadService, smsQueuePublisher);
		this.messagingIntegration = messagingIntegration;
	}

	public MessageResult deliverSms(final MessageEntity messageEntity, final RecipientEntity recipientEntity) {
		return deliver(messageEntity, recipientEntity, Map.of());
	}

	@Override
	protected MessageResult sendOverRest(final MessageEntity messageEntity, final RecipientEntity recipientEntity, final Map<String, String> settingsMap) {
		return messagingIntegration.sendSms(messageEntity, recipientEntity);
	}

	@Override
	protected SmsQueueMessage toQueueMessage(final MessageEntity messageEntity, final RecipientEntity recipientEntity,
		final Map<String, String> settingsMap, final List<String> objectIds) {
		return toSmsQueueMessage(messageEntity, recipientEntity);
	}

	/**
	 * An SMS is its body and nothing else. Uploading a letter's attachments on this channel would put objects in the
	 * store that no message ever names, and they would sit there until their TTL expired.
	 */
	@Override
	protected boolean carriesAttachments() {
		return false;
	}
}
