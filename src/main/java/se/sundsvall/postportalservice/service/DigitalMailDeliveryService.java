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
import se.sundsvall.postportalservice.integration.rabbitmq.DigitalMailQueueMessage;
import se.sundsvall.postportalservice.integration.rabbitmq.DigitalMailQueuePublisher;

import static se.sundsvall.postportalservice.integration.rabbitmq.RabbitMapper.toDigitalMailQueueMessage;

/**
 * Sends digital mail to a single recipient. Behaviour lives in {@link QueuedDeliveryService}.
 * <p>
 * The REST path answers with a batch result because the endpoint takes a list of parties; this service asks for the
 * first delivery in it, which is the only one there can be for a request naming one recipient.
 */
@Service
public class DigitalMailDeliveryService extends QueuedDeliveryService<DigitalMailQueueMessage> {

	private final MessagingIntegration messagingIntegration;

	public DigitalMailDeliveryService(
		final MessagingIntegration messagingIntegration,
		final RecipientRepository recipientRepository,
		final AttachmentUploadService attachmentUploadService,
		final Optional<DigitalMailQueuePublisher> digitalMailQueuePublisher) {
		super(recipientRepository, attachmentUploadService, digitalMailQueuePublisher);
		this.messagingIntegration = messagingIntegration;
	}

	public MessageResult deliverDigitalMail(final MessageEntity messageEntity, final RecipientEntity recipientEntity) {
		return deliver(messageEntity, recipientEntity, Map.of());
	}

	@Override
	protected MessageResult sendOverRest(final MessageEntity messageEntity, final RecipientEntity recipientEntity, final Map<String, String> settingsMap) {
		return messagingIntegration.sendDigitalMail(messageEntity, recipientEntity).getMessages().getFirst();
	}

	@Override
	protected DigitalMailQueueMessage toQueueMessage(final MessageEntity messageEntity, final RecipientEntity recipientEntity,
		final Map<String, String> settingsMap, final List<String> objectIds) {
		return toDigitalMailQueueMessage(messageEntity, recipientEntity, objectIds);
	}
}
