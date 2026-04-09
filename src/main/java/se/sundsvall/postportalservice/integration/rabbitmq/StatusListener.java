package se.sundsvall.postportalservice.integration.rabbitmq;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import se.sundsvall.postportalservice.integration.digitalregisteredletter.DigitalRegisteredLetterIntegration;

import static se.sundsvall.postportalservice.integration.rabbitmq.Constants.STATUS_REGISTERED_LETTER_QUEUE;

@Component
public class StatusListener implements Listener {

	private final DigitalRegisteredLetterIntegration digitalRegisteredLetterIntegration;

	public StatusListener(final DigitalRegisteredLetterIntegration digitalRegisteredLetterIntegration) {
		this.digitalRegisteredLetterIntegration = digitalRegisteredLetterIntegration;
	}

	@Override
	@RabbitListener(queues = STATUS_REGISTERED_LETTER_QUEUE)
	public void handleEvent(final String externalId) {

		// digitalRegisteredLetterIntegration.getLetterStatuses()

	}

}
