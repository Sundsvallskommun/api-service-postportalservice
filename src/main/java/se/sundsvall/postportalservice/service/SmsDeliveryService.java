package se.sundsvall.postportalservice.service;

import generated.se.sundsvall.messaging.MessageResult;
import java.util.Optional;
import org.springframework.stereotype.Service;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.db.dao.RecipientRepository;
import se.sundsvall.postportalservice.integration.messaging.MessagingIntegration;
import se.sundsvall.postportalservice.integration.rabbitmq.SmsQueuePublisher;
import se.sundsvall.postportalservice.service.util.RecipientId;

import static se.sundsvall.postportalservice.Constants.PENDING;
import static se.sundsvall.postportalservice.integration.rabbitmq.RabbitMapper.toSmsQueueMessage;

/**
 * Sends a single SMS, either by publishing it onto the messaging service's queue or, when the queue path is switched
 * off, by calling the messaging REST API as before.
 * <p>
 * The two paths report their outcome differently. The REST call answers with a {@link MessageResult} the caller applies
 * straight away. The queue path answers with nothing: messaging retries a transient failure through its own backoff
 * ladder, so the outcome arrives later on the status queue and is applied by {@link SmsStatusService#handleSmsStatus}.
 * Until then the recipient stays PENDING. A publish the broker does not confirm raises instead, which leaves the
 * recipient FAILED via the caller's catch block.
 */
@Service
public class SmsDeliveryService {

	private final MessagingIntegration messagingIntegration;
	private final RecipientRepository recipientRepository;
	// Absent unless rabbitmq.enabled=true, in which case SMS goes over the queue instead of the messaging REST API.
	private final Optional<SmsQueuePublisher> smsQueuePublisher;

	public SmsDeliveryService(
		final MessagingIntegration messagingIntegration,
		final RecipientRepository recipientRepository,
		final Optional<SmsQueuePublisher> smsQueuePublisher) {
		this.messagingIntegration = messagingIntegration;
		this.recipientRepository = recipientRepository;
		this.smsQueuePublisher = smsQueuePublisher;
	}

	public MessageResult deliverSms(final MessageEntity messageEntity, final RecipientEntity recipientEntity) {
		if (smsQueuePublisher.isEmpty()) {
			return messagingIntegration.sendSms(messageEntity, recipientEntity);
		}

		RecipientId.init(recipientEntity.getId());

		// PENDING is written before the publish, not after: messaging can be quick enough that the status message
		// arrives while we are still here, and a later write of PENDING would overwrite the outcome it carried.
		recipientEntity.setStatus(PENDING);
		recipientRepository.save(recipientEntity);

		smsQueuePublisher.get().publish(toSmsQueueMessage(messageEntity, recipientEntity));
		return null;
	}
}
