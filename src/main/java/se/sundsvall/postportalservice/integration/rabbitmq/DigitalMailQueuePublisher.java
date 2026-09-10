package se.sundsvall.postportalservice.integration.rabbitmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import se.sundsvall.postportalservice.integration.rabbitmq.RabbitIntegrationConfiguration.RabbitIntegrationProperties;

/**
 * Publishes digital mail onto the messaging service's inbound exchange with routing key {@code digital-mail}. Behaviour
 * lives in
 * {@link QueuePublisher}.
 * <p>
 * The bean's presence is the channel's feature flag: it exists only when {@code rabbitmq.enabled=true}, and the
 * delivery service takes it as an {@link java.util.Optional} so an absent one means the REST path.
 */
@Component
@ConditionalOnProperty(name = "rabbitmq.enabled", havingValue = "true")
public class DigitalMailQueuePublisher extends QueuePublisher<DigitalMailQueueMessage> {

	public DigitalMailQueuePublisher(final RabbitTemplate rabbitTemplate, final RabbitIntegrationProperties properties) {
		super(rabbitTemplate, properties, properties.digitalMail(), "Digital mail");
	}
}
