package se.sundsvall.postportalservice.integration.rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import se.sundsvall.postportalservice.service.EmailStatusService;
import se.sundsvall.postportalservice.service.util.RecipientId;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.springframework.amqp.support.AmqpHeaders.RECEIVED_ROUTING_KEY;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.postportalservice.Constants.FAILED;
import static se.sundsvall.postportalservice.Constants.SENT;

/**
 * Consumes the delivery outcomes the messaging service publishes back for e-mail sent over the queue.
 * <p>
 * The queue is declared externally by the messaging-topology-operator, so this uses {@code queues} and never
 * {@code queuesToDeclare} - see {@link RabbitIntegrationConfiguration}.
 * <p>
 * The outcome reaches us twice: once as the routing key messaging published on, and once in the payload. The payload
 * wins - it is the only one of the two that survives the whole journey, since dead-lettering this queue rewrites the
 * routing key to the parking lot's name. The key is the fallback for a payload that carries no outcome we recognise,
 * and a disagreement between the two is logged.
 */
@Component
@ConditionalOnProperty(name = "rabbitmq.enabled", havingValue = "true")
class EmailStatusListener {

	private static final Logger LOG = LoggerFactory.getLogger(EmailStatusListener.class);

	static final String SENT_ROUTING_KEY = "email.sent";
	static final String FAILED_ROUTING_KEY = "email.failed";

	private final EmailStatusService emailStatusService;

	EmailStatusListener(final EmailStatusService emailStatusService) {
		this.emailStatusService = emailStatusService;
	}

	@RabbitListener(queues = "${rabbitmq.email.status-queue}")
	void receive(
		@Payload final EmailStatusMessage emailStatusMessage,
		@Header(name = RECEIVED_ROUTING_KEY, required = false) final String routingKey) {

		RecipientId.init(emailStatusMessage.recipientId());
		try {
			final var resolved = resolve(emailStatusMessage, routingKey);
			LOG.info("Received e-mail outcome {} for recipient {}", resolved.status(), resolved.recipientId());
			emailStatusService.handleEmailStatus(resolved);
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
	EmailStatusMessage resolve(final EmailStatusMessage emailStatusMessage, final String routingKey) {
		final var fromPayload = toOutcome(emailStatusMessage.status());
		final var fromRoutingKey = toRoutingKeyOutcome(routingKey);

		if (fromPayload == null) {
			// The routing key is worth trusting here only because the payload offered nothing to trust instead.
			final var outcome = ofNullable(fromRoutingKey).orElse(FAILED);
			final var reported = sanitizeForLogging(emailStatusMessage.status());
			LOG.warn("Unrecognised e-mail outcome '{}' for recipient {}, recording {}", reported, emailStatusMessage.recipientId(), outcome);

			return withOutcome(emailStatusMessage, outcome, "Unrecognised outcome '%s' reported by messaging".formatted(reported));
		}

		if (fromRoutingKey != null && !fromRoutingKey.equals(fromPayload)) {
			LOG.warn("Routing key {} disagrees with payload status {} for recipient {}, acting on the payload",
				sanitizeForLogging(routingKey), sanitizeForLogging(emailStatusMessage.status()), emailStatusMessage.recipientId());
		}

		if (fromPayload.equals(emailStatusMessage.status())) {
			return emailStatusMessage;
		}
		return withOutcome(emailStatusMessage, fromPayload, emailStatusMessage.statusDetail());
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

	private static String toRoutingKeyOutcome(final String routingKey) {
		return switch (trimToEmpty(routingKey)) {
			case SENT_ROUTING_KEY -> SENT;
			case FAILED_ROUTING_KEY -> FAILED;
			default -> null;
		};
	}

	private static EmailStatusMessage withOutcome(final EmailStatusMessage emailStatusMessage, final String status, final String statusDetail) {
		return new EmailStatusMessage(emailStatusMessage.recipientId(), status, emailStatusMessage.externalId(), statusDetail);
	}
}
