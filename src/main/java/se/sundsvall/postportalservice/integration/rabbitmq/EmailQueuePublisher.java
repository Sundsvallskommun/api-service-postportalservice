package se.sundsvall.postportalservice.integration.rabbitmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import se.sundsvall.postportalservice.integration.rabbitmq.RabbitIntegrationConfiguration.RabbitIntegrationProperties;

/**
 * Publishes an e-mail onto the messaging service's inbound exchange with routing key {@code email}. Behaviour lives in
 * {@link QueuePublisher}.
 * <p>
 * The bean's presence is the channel's feature flag: it exists only when {@code rabbitmq.enabled=true}, and the
 * delivery service takes it as an {@link java.util.Optional} so an absent one means the REST path.
 */
@Component
@ConditionalOnProperty(name = "rabbitmq.enabled", havingValue = "true")
public class EmailQueuePublisher extends QueuePublisher<EmailQueueMessage> {

	public EmailQueuePublisher(final RabbitTemplate rabbitTemplate, final RabbitIntegrationProperties properties) {
		super(rabbitTemplate, properties, properties.email(), "E-mail");
	}
}
