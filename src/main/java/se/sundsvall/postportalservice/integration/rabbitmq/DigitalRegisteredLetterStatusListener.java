package se.sundsvall.postportalservice.integration.rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import se.sundsvall.postportalservice.integration.db.dao.RecipientRepository;
import se.sundsvall.postportalservice.integration.rabbitmq.model.DigitalRegisteredLetterStatusEvent;

import static se.sundsvall.postportalservice.integration.rabbitmq.model.Constants.STATUS_DIGITAL_REGISTERED_LETTER_QUEUE;

@Component
public class DigitalRegisteredLetterStatusListener {

	private static final Logger LOG = LoggerFactory.getLogger(DigitalRegisteredLetterStatusListener.class);

	private final RecipientRepository recipientRepository;

	public DigitalRegisteredLetterStatusListener(final RecipientRepository recipientRepository) {
		this.recipientRepository = recipientRepository;
	}

	@RabbitListener(queues = STATUS_DIGITAL_REGISTERED_LETTER_QUEUE)
	public void handleStatusEvent(final DigitalRegisteredLetterStatusEvent event) {
		LOG.info("Received status event for recipient with id {}", event.recipientId());

		final var recipient = recipientRepository.findById(event.recipientId())
			.orElseThrow(() -> {
				LOG.error("Recipient with id {} not found", event.recipientId());
				return new IllegalArgumentException("Recipient with id '%s' not found".formatted(event.recipientId()));
			});

		recipient.setStatus(event.status());
		recipient.setExternalId(event.externalId());
		recipient.setStatusDetail(event.statusDetail());

		recipientRepository.save(recipient);

		LOG.info("Updated recipient with id {}, status: {}, externalId: {}", event.recipientId(), event.status(), event.externalId());
	}
}
