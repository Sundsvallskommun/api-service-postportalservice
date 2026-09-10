package se.sundsvall.postportalservice.integration.rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sundsvall.postportalservice.service.RecipientStatusService;
import se.sundsvall.postportalservice.service.util.RecipientId;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.postportalservice.Constants.FAILED;
import static se.sundsvall.postportalservice.Constants.SENT;

/**
 * Consumes the delivery outcomes the messaging service publishes back for one channel.
 * <p>
 * The queues are declared externally by the messaging-topology-operator, so subclasses use {@code queues} and never
 * {@code queuesToDeclare} - see {@link RabbitIntegrationConfiguration}. They carry the
 * {@link org.springframework.amqp.rabbit.annotation.RabbitListener} annotation themselves, because the queue name is a
 * per-channel property placeholder and an inherited annotation could only name one queue.
 * <p>
 * The outcome reaches us twice: once as the routing key messaging published on, and once in the payload. The payload
 * wins - it is the only one of the two that survives the whole journey, since dead-lettering this queue rewrites the
 * routing key to the parking lot's name. The key is the fallback for a payload that carries no outcome we recognise,
 * and a disagreement between the two is logged.
 */
public abstract class StatusListener {

	private static final Logger LOG = LoggerFactory.getLogger(StatusListener.class);

	private final RecipientStatusService recipientStatusService;
	private final String channel;
	private final String sentRoutingKey;
	private final String failedRoutingKey;

	/**
	 * @param channel the channel's name as it should read in a log line, e.g. {@code "e-mail"}
	 */
	protected StatusListener(final RecipientStatusService recipientStatusService, final String channel,
		final String sentRoutingKey, final String failedRoutingKey) {
		this.recipientStatusService = recipientStatusService;
		this.channel = channel;
		this.sentRoutingKey = sentRoutingKey;
		this.failedRoutingKey = failedRoutingKey;
	}

	protected final void handle(final StatusMessage statusMessage, final String routingKey) {
		RecipientId.init(statusMessage.recipientId());
		try {
			final var resolved = resolve(statusMessage, routingKey);
			LOG.info("Received {} outcome {} for recipient {}", channel, resolved.status(), resolved.recipientId());
			recipientStatusService.handleStatus(resolved);
		} finally {
			RecipientId.reset();
		}
	}

	/**
	 * Reduces whatever arrived to one of the two terminal outcomes.
	 * <p>
	 * Nothing here refuses a message. Refusing one would requeue it until the delivery limit parks it, leaving the
	 * recipient at PENDING with no second outcome ever coming - the precise failure the two-way flow exists to remove.
	 * A malformed outcome is therefore recorded as FAILED, with what actually arrived kept in the status detail.
	 */
	StatusMessage resolve(final StatusMessage statusMessage, final String routingKey) {
		final var fromPayload = toOutcome(statusMessage.status());
		final var fromRoutingKey = toRoutingKeyOutcome(routingKey);

		if (fromPayload == null) {
			// The routing key is worth trusting here only because the payload offered nothing to trust instead.
			final var outcome = ofNullable(fromRoutingKey).orElse(FAILED);
			final var reported = sanitizeForLogging(statusMessage.status());
			LOG.warn("Unrecognised {} outcome '{}' for recipient {}, recording {}", channel, reported, statusMessage.recipientId(), outcome);

			return DeliveryOutcome.of(statusMessage, outcome, "Unrecognised outcome '%s' reported by messaging".formatted(reported));
		}

		if (fromRoutingKey != null && !fromRoutingKey.equals(fromPayload)) {
			LOG.warn("Routing key {} disagrees with payload status {} for recipient {}, acting on the payload",
				sanitizeForLogging(routingKey), sanitizeForLogging(statusMessage.status()), statusMessage.recipientId());
		}

		if (fromPayload.equals(statusMessage.status())) {
			return statusMessage;
		}
		return DeliveryOutcome.of(statusMessage, fromPayload, statusMessage.statusDetail());
	}

	/**
	 * Liberal in what it recognises, strict in what it stores: casing and stray whitespace are accepted, anything else
	 * is not an outcome.
	 */
	private static String toOutcome(final String status) {
		return switch (trimToEmpty(status).toUpperCase()) {
			case SENT -> SENT;
			case FAILED -> FAILED;
			default -> null;
		};
	}

	private String toRoutingKeyOutcome(final String routingKey) {
		final var trimmed = trimToEmpty(routingKey);

		if (sentRoutingKey.equals(trimmed)) {
			return SENT;
		}
		if (failedRoutingKey.equals(trimmed)) {
			return FAILED;
		}
		return null;
	}
}
