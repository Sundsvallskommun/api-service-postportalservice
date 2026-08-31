package se.sundsvall.postportalservice.integration.rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import se.sundsvall.postportalservice.service.SmsStatusService;
import se.sundsvall.postportalservice.service.util.RecipientId;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.springframework.amqp.support.AmqpHeaders.RECEIVED_ROUTING_KEY;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.postportalservice.Constants.FAILED;
import static se.sundsvall.postportalservice.Constants.SENT;

/**
 * Consumes the delivery outcomes the messaging service publishes back for SMS sent over the queue.
 * <p>
 * The queue is declared externally by the messaging-topology-operator, so this uses {@code queues} and never
 * {@code queuesToDeclare} - see {@link RabbitIntegrationConfiguration}.
 * <p>
 * This runs on a listener container thread, not on the delivery pool that published the message, so the recipient id
 * has to be put back into the MDC here for the log lines to correlate with the publish.
 * <p>
 * The outcome reaches us twice: once as the routing key messaging published on, and once in the payload. The payload
 * wins - it is the only one of the two that survives the whole journey, since dead-lettering this queue rewrites the
 * routing key to the parking lot's name. The key is the fallback for a payload that carries no outcome we recognise,
 * and a disagreement between the two is logged.
 * <p>
 * Whatever arrives, the recipient row is only ever written SENT or FAILED. The contract has exactly those two terminal
 * outcomes, and {@code status} is passed straight out of the History API, so an unrecognised token would otherwise
 * reach callers as a status they have no way to interpret.
 */
@Component
@ConditionalOnProperty(name = "rabbitmq.enabled", havingValue = "true")
class SmsStatusListener {

	private static final Logger LOG = LoggerFactory.getLogger(SmsStatusListener.class);

	static final String SENT_ROUTING_KEY = "sms.sent";
	static final String FAILED_ROUTING_KEY = "sms.failed";

	private final SmsStatusService smsStatusService;

	SmsStatusListener(final SmsStatusService smsStatusService) {
		this.smsStatusService = smsStatusService;
	}

	@RabbitListener(queues = "${rabbitmq.status-queue}")
	void receive(
		@Payload final SmsStatusMessage smsStatusMessage,
		@Header(name = RECEIVED_ROUTING_KEY, required = false) final String routingKey) {

		RecipientId.init(smsStatusMessage.recipientId());
		try {
			final var resolved = resolve(smsStatusMessage, routingKey);
			LOG.info("Received SMS outcome {} for recipient {}", resolved.status(), resolved.recipientId());
			smsStatusService.handleSmsStatus(resolved);
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
	SmsStatusMessage resolve(final SmsStatusMessage smsStatusMessage, final String routingKey) {
		final var fromPayload = toOutcome(smsStatusMessage.status());
		final var fromRoutingKey = switch (trimToEmpty(routingKey)) {
			case SENT_ROUTING_KEY -> SENT;
			case FAILED_ROUTING_KEY -> FAILED;
			default -> null;
		};

		if (fromPayload == null) {
			// The routing key is worth trusting here only because the payload offered nothing to trust instead.
			final var outcome = ofNullable(fromRoutingKey).orElse(FAILED);
			LOG.warn("Unrecognised SMS outcome '{}' for recipient {}, recording {}",
				sanitizeForLogging(smsStatusMessage.status()), smsStatusMessage.recipientId(), outcome);

			return new SmsStatusMessage(smsStatusMessage.recipientId(), outcome, smsStatusMessage.externalId(),
				"Unrecognised outcome '%s' reported by messaging".formatted(sanitizeForLogging(smsStatusMessage.status())));
		}

		if (fromRoutingKey != null && !fromRoutingKey.equals(fromPayload)) {
			LOG.warn("Routing key {} disagrees with payload status {} for recipient {}, acting on the payload",
				sanitizeForLogging(routingKey), sanitizeForLogging(smsStatusMessage.status()), smsStatusMessage.recipientId());
		}

		return fromPayload.equals(smsStatusMessage.status())
			? smsStatusMessage
			: new SmsStatusMessage(smsStatusMessage.recipientId(), fromPayload, smsStatusMessage.externalId(), smsStatusMessage.statusDetail());
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
}
