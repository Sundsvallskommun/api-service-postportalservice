package se.sundsvall.postportalservice.integration.rabbitmq;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.postportalservice.integration.rabbitmq.RabbitIntegrationConfiguration.RabbitIntegrationProperties;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;

/**
 * Publishes one recipient's send request onto the messaging service's inbound exchange.
 * <p>
 * This is the primary delivery path on every channel that uses it, so a failure must never be swallowed: a dropped
 * message is a letter the recipient never receives, with nothing in the database to say so. Every publish therefore
 * waits for a broker confirmation and raises on a NACK, an unroutable return or a timeout, which leaves the caller to
 * mark the recipient FAILED.
 * <p>
 * The exchange is shared across channels - it is messaging's hub and routes by key - so the only per-channel part is
 * the routing key, which is why subclasses are one constructor each.
 *
 * @param <T> this channel's queue payload
 */
public abstract class QueuePublisher<T extends QueueMessage> {

	private static final Logger LOG = LoggerFactory.getLogger(QueuePublisher.class);

	private final RabbitTemplate rabbitTemplate;
	private final RabbitIntegrationProperties properties;
	private final RabbitIntegrationProperties.Channel channel;
	private final String label;

	/**
	 * @param label the channel's name as it should read in a log line or a problem detail, e.g. {@code "E-mail"}
	 */
	protected QueuePublisher(final RabbitTemplate rabbitTemplate, final RabbitIntegrationProperties properties,
		final RabbitIntegrationProperties.Channel channel, final String label) {
		this.rabbitTemplate = rabbitTemplate;
		this.properties = properties;
		this.channel = channel;
		this.label = label;
	}

	public void publish(final T queueMessage) {
		final var exchange = properties.exchange();
		final var routingKey = channel.routingKey();
		final var recipientId = queueMessage.recipientId();
		final var correlationData = new CorrelationData(recipientId);

		rabbitTemplate.convertAndSend(exchange, routingKey, queueMessage, correlationData);

		final var confirm = awaitConfirm(correlationData, recipientId);

		// A basic.return always precedes the basic.ack, so by now an unroutable message has been handed back to us.
		Optional.ofNullable(correlationData.getReturned()).ifPresent(returned -> {
			throw Problem.valueOf(BAD_GATEWAY, "%s for recipient %s was not routable by exchange %s with routing key %s: %s"
				.formatted(label, recipientId, exchange, routingKey, returned.getReplyText()));
		});

		if (!confirm.ack()) {
			throw Problem.valueOf(BAD_GATEWAY, "%s for recipient %s was rejected by the broker: %s".formatted(label, recipientId, confirm.reason()));
		}

		LOG.info("Published {} for recipient {} (exchange={}, routingKey={})", label, recipientId, exchange, routingKey);
	}

	private CorrelationData.Confirm awaitConfirm(final CorrelationData correlationData, final String recipientId) {
		try {
			return correlationData.getFuture().get(properties.publishConfirmTimeoutSeconds(), SECONDS);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			throw Problem.valueOf(BAD_GATEWAY, "Interrupted while waiting for broker confirmation of %s for recipient %s".formatted(label, recipientId));
		} catch (final ExecutionException | TimeoutException _) {
			throw Problem.valueOf(BAD_GATEWAY, "No broker confirmation of %s for recipient %s within %d seconds"
				.formatted(label, recipientId, properties.publishConfirmTimeoutSeconds()));
		}
	}
}
