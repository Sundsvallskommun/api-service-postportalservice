package se.sundsvall.postportalservice.integration.rabbitmq;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import se.sundsvall.postportalservice.service.RecipientStatusService;

import static org.springframework.amqp.support.AmqpHeaders.RECEIVED_ROUTING_KEY;

/**
 * Consumes an e-mail delivery outcomes. Behaviour lives in {@link StatusListener}.
 */
@Component
@ConditionalOnProperty(name = "rabbitmq.enabled", havingValue = "true")
class EmailStatusListener extends StatusListener {

	static final String SENT_ROUTING_KEY = "email.sent";
	static final String FAILED_ROUTING_KEY = "email.failed";

	EmailStatusListener(final RecipientStatusService recipientStatusService) {
		super(recipientStatusService, "e-mail", SENT_ROUTING_KEY, FAILED_ROUTING_KEY);
	}

	@RabbitListener(queues = "${rabbitmq.email.status-queue}")
	void receive(
		@Payload final EmailStatusMessage statusMessage,
		@Header(name = RECEIVED_ROUTING_KEY, required = false) final String routingKey) {

		handle(statusMessage, routingKey);
	}
}
