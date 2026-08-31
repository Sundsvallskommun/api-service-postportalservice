package se.sundsvall.postportalservice.apptest.support;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import se.sundsvall.postportalservice.integration.rabbitmq.SmsQueueMessage;
import se.sundsvall.postportalservice.integration.rabbitmq.SmsStatusMessage;

import static se.sundsvall.postportalservice.Constants.FAILED;
import static se.sundsvall.postportalservice.Constants.SENT;

/**
 * Stands in for the messaging service in {@code MessageSmsIT}: consumes from the work queue and publishes a delivery
 * outcome back onto the status exchange, which is the round trip the real service is expected to make.
 * <p>
 * Which recipients succeed is decided per test by mobile number, the way the WireMock stubs used to decide it by
 * request body before SMS moved onto the queue.
 */
public class MessagingQueueStub {

	public static final String WORK_EXCHANGE = "api-fabriken.messaging";
	public static final String WORK_QUEUE = "api-fabriken.messaging.sms";
	public static final String WORK_ROUTING_KEY = "sms";
	// The outcome hub belongs to messaging, the same way the inbound hub does; postportal owns only the queue it
	// consumes and the binding that fills it. The outcome is carried by the routing key as well as the payload.
	public static final String STATUS_EXCHANGE = "api-fabriken.messaging.status";
	public static final String STATUS_QUEUE = "api-fabriken.postportal.sms-status";
	public static final String STATUS_BINDING_PATTERN = "sms.*";
	public static final String SENT_ROUTING_KEY = "sms.sent";
	public static final String FAILED_ROUTING_KEY = "sms.failed";

	private static final Logger LOG = LoggerFactory.getLogger(MessagingQueueStub.class);

	private final RabbitTemplate rabbitTemplate;
	private final List<SmsQueueMessage> received = new CopyOnWriteArrayList<>();

	private volatile Set<String> failingMobileNumbers = Set.of();
	private volatile boolean silent = false;

	public MessagingQueueStub(final RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	@RabbitListener(queues = WORK_QUEUE)
	void consume(final SmsQueueMessage smsQueueMessage) {
		LOG.info("Stub consumed SMS for recipient {}", smsQueueMessage.recipientId());
		received.add(smsQueueMessage);

		if (silent) {
			// Models messaging still working through its retry ladder: nothing has come back yet.
			return;
		}

		final var failed = failingMobileNumbers.contains(smsQueueMessage.mobileNumber());
		publishOutcome(smsQueueMessage.recipientId(), failed);
	}

	/**
	 * Publishes one terminal outcome. Exposed so a test can send the same outcome twice, which is what a crash between
	 * messaging's publish and its ack looks like from this side.
	 */
	public void publishOutcome(final String recipientId, final boolean failed) {
		rabbitTemplate.convertAndSend(
			STATUS_EXCHANGE,
			failed ? FAILED_ROUTING_KEY : SENT_ROUTING_KEY,
			new SmsStatusMessage(
				recipientId,
				failed ? FAILED : SENT,
				failed ? null : UUID.randomUUID().toString(),
				failed ? "Invalid mobile number" : null));
	}

	public void reset() {
		received.clear();
		failingMobileNumbers = Set.of();
		silent = false;
	}

	public void failFor(final String... mobileNumbers) {
		failingMobileNumbers = Set.of(mobileNumbers);
	}

	public void stayQuiet() {
		silent = true;
	}

	public List<SmsQueueMessage> getReceived() {
		return List.copyOf(received);
	}
}
