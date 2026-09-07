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
 * Publishes an e-mail onto the messaging service's inbound exchange.
 * <p>
 * As with SMS this is the primary delivery path, so a failure must never be swallowed: a dropped message is a mail the
 * recipient never receives, with nothing in the database to say so. Every publish therefore waits for a broker
 * confirmation and raises on a NACK, an unroutable return or a timeout, which leaves the caller to mark the recipient
 * FAILED.
 */
@Component
@ConditionalOnProperty(name = "rabbitmq.enabled", havingValue = "true")
public class EmailQueuePublisher {

	private static final Logger LOG = LoggerFactory.getLogger(EmailQueuePublisher.class);

	private final RabbitTemplate rabbitTemplate;
	private final RabbitIntegrationProperties properties;

	public EmailQueuePublisher(final RabbitTemplate rabbitTemplate, final RabbitIntegrationProperties properties) {
		this.rabbitTemplate = rabbitTemplate;
		this.properties = properties;
	}

	public void publish(final EmailQueueMessage emailQueueMessage) {
		final var exchange = properties.exchange();
		final var routingKey = properties.email().routingKey();
		final var recipientId = emailQueueMessage.recipientId();
		final var correlationData = new CorrelationData(recipientId);

		rabbitTemplate.convertAndSend(exchange, routingKey, emailQueueMessage, correlationData);

		final var confirm = awaitConfirm(correlationData, recipientId);

		// A basic.return always precedes the basic.ack, so by now an unroutable message has been handed back to us.
		Optional.ofNullable(correlationData.getReturned()).ifPresent(returned -> {
			throw Problem.valueOf(BAD_GATEWAY, "E-mail for recipient %s was not routable by exchange %s with routing key %s: %s"
				.formatted(recipientId, exchange, routingKey, returned.getReplyText()));
		});

		if (!confirm.ack()) {
			throw Problem.valueOf(BAD_GATEWAY, "E-mail for recipient %s was rejected by the broker: %s".formatted(recipientId, confirm.reason()));
		}

		LOG.info("Published e-mail for recipient {} (exchange={}, routingKey={})", recipientId, exchange, routingKey);
	}

	private CorrelationData.Confirm awaitConfirm(final CorrelationData correlationData, final String recipientId) {
		try {
			return correlationData.getFuture().get(properties.publishConfirmTimeoutSeconds(), SECONDS);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			throw Problem.valueOf(BAD_GATEWAY, "Interrupted while waiting for broker confirmation of e-mail for recipient %s".formatted(recipientId));
		} catch (final ExecutionException | TimeoutException e) {
			throw Problem.valueOf(BAD_GATEWAY, "No broker confirmation of e-mail for recipient %s within %d seconds"
				.formatted(recipientId, properties.publishConfirmTimeoutSeconds()));
		}
	}
}
