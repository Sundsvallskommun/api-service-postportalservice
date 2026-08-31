package se.sundsvall.postportalservice.integration.rabbitmq;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.postportalservice.integration.rabbitmq.RabbitIntegrationConfiguration.RabbitIntegrationProperties;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;

/**
 * Publishes an SMS onto the messaging service's inbound exchange.
 * <p>
 * Unlike messaging's own outbound mirror this is the primary delivery path, so a failure must never be swallowed: a
 * dropped message is a text message the recipient never receives, with nothing in the database to say so. Every publish
 * therefore waits for a broker confirmation and raises on a NACK, an unroutable return or a timeout, which leaves the
 * caller to mark the recipient FAILED.
 */
@Component
@ConditionalOnProperty(name = "rabbitmq.enabled", havingValue = "true")
public class SmsQueuePublisher {

	private static final Logger LOG = LoggerFactory.getLogger(SmsQueuePublisher.class);

	private final RabbitTemplate rabbitTemplate;
	private final RabbitIntegrationProperties properties;

	public SmsQueuePublisher(final RabbitTemplate rabbitTemplate, final RabbitIntegrationProperties properties) {
		this.rabbitTemplate = rabbitTemplate;
		this.properties = properties;
	}

	public void publish(final SmsQueueMessage smsQueueMessage) {
		final var exchange = properties.exchange();
		final var routingKey = properties.routingKey();
		final var recipientId = smsQueueMessage.recipientId();
		final var correlationData = new CorrelationData(recipientId);

		rabbitTemplate.convertAndSend(exchange, routingKey, smsQueueMessage, correlationData);

		final var confirm = awaitConfirm(correlationData, recipientId);

		// A basic.return always precedes the basic.ack, so by now an unroutable message has been handed back to us.
		Optional.ofNullable(correlationData.getReturned()).ifPresent(returned -> {
			throw Problem.valueOf(BAD_GATEWAY, "SMS for recipient %s was not routable by exchange %s with routing key %s: %s"
				.formatted(recipientId, exchange, routingKey, returned.getReplyText()));
		});

		if (!confirm.ack()) {
			throw Problem.valueOf(BAD_GATEWAY, "SMS for recipient %s was rejected by the broker: %s".formatted(recipientId, confirm.reason()));
		}

		LOG.info("Published SMS for recipient {} (exchange={}, routingKey={})", recipientId, exchange, routingKey);
	}

	private CorrelationData.Confirm awaitConfirm(final CorrelationData correlationData, final String recipientId) {
		try {
			return correlationData.getFuture().get(properties.publishConfirmTimeoutSeconds(), SECONDS);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			throw Problem.valueOf(BAD_GATEWAY, "Interrupted while waiting for broker confirmation of SMS for recipient %s".formatted(recipientId));
		} catch (final ExecutionException | TimeoutException e) {
			throw Problem.valueOf(BAD_GATEWAY, "No broker confirmation of SMS for recipient %s within %d seconds"
				.formatted(recipientId, properties.publishConfirmTimeoutSeconds()));
		}
	}
}
