package se.sundsvall.postportalservice.apptest.support;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import se.sundsvall.postportalservice.integration.rabbitmq.DigitalMailQueueMessage;
import se.sundsvall.postportalservice.integration.rabbitmq.DigitalMailStatusMessage;
import se.sundsvall.postportalservice.integration.rabbitmq.EmailQueueMessage;
import se.sundsvall.postportalservice.integration.rabbitmq.EmailStatusMessage;
import se.sundsvall.postportalservice.integration.rabbitmq.SmsQueueMessage;
import se.sundsvall.postportalservice.integration.rabbitmq.SmsStatusMessage;
import se.sundsvall.postportalservice.integration.rabbitmq.SnailMailQueueMessage;
import se.sundsvall.postportalservice.integration.rabbitmq.SnailMailStatusMessage;

import static se.sundsvall.postportalservice.Constants.FAILED;
import static se.sundsvall.postportalservice.Constants.SENT;

/**
 * Stands in for the messaging service: consumes from the work queues and publishes a delivery outcome back onto the
 * status exchange, which is the round trip the real service is expected to make.
 * <p>
 * Which recipients succeed is decided per test - by mobile number or e-mail address on the two channels that address a
 * recipient directly, and by party id on the two that do not - the way the WireMock stubs used to decide it by request
 * body before these channels moved onto the queue.
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

	public static final String EMAIL_WORK_QUEUE = "api-fabriken.messaging.email";
	public static final String EMAIL_WORK_ROUTING_KEY = "email";
	public static final String EMAIL_STATUS_QUEUE = "api-fabriken.postportal.email-status";
	public static final String EMAIL_STATUS_BINDING_PATTERN = "email.*";
	public static final String EMAIL_SENT_ROUTING_KEY = "email.sent";
	public static final String EMAIL_FAILED_ROUTING_KEY = "email.failed";

	public static final String DIGITAL_MAIL_WORK_QUEUE = "api-fabriken.messaging.digital-mail";
	public static final String DIGITAL_MAIL_WORK_ROUTING_KEY = "digital-mail";
	public static final String DIGITAL_MAIL_STATUS_QUEUE = "api-fabriken.postportal.digital-mail-status";
	public static final String DIGITAL_MAIL_STATUS_BINDING_PATTERN = "digital-mail.*";
	public static final String DIGITAL_MAIL_SENT_ROUTING_KEY = "digital-mail.sent";
	public static final String DIGITAL_MAIL_FAILED_ROUTING_KEY = "digital-mail.failed";

	public static final String SNAIL_MAIL_WORK_QUEUE = "api-fabriken.messaging.snail-mail";
	public static final String SNAIL_MAIL_WORK_ROUTING_KEY = "snail-mail";
	public static final String SNAIL_MAIL_STATUS_QUEUE = "api-fabriken.postportal.snail-mail-status";
	public static final String SNAIL_MAIL_STATUS_BINDING_PATTERN = "snail-mail.*";
	public static final String SNAIL_MAIL_SENT_ROUTING_KEY = "snail-mail.sent";
	public static final String SNAIL_MAIL_FAILED_ROUTING_KEY = "snail-mail.failed";

	private static final Logger LOG = LoggerFactory.getLogger(MessagingQueueStub.class);

	private final RabbitTemplate rabbitTemplate;
	private final List<SmsQueueMessage> received = new CopyOnWriteArrayList<>();
	private final List<EmailQueueMessage> receivedEmails = new CopyOnWriteArrayList<>();
	private final List<DigitalMailQueueMessage> receivedDigitalMail = new CopyOnWriteArrayList<>();
	private final List<SnailMailQueueMessage> receivedSnailMail = new CopyOnWriteArrayList<>();

	private volatile Set<String> failingMobileNumbers = Set.of();
	private volatile Set<String> failingEmailAddresses = Set.of();
	private volatile Set<String> failingPartyIds = Set.of();
	private volatile boolean silent = false;

	public MessagingQueueStub(final RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	/** Builds one channel's outcome record; the four are shaped alike but are separate wire contracts. */
	@FunctionalInterface
	private interface StatusMessageFactory {
		Object create(String recipientId, String status, String externalId, String statusDetail);
	}

	@RabbitListener(queues = WORK_QUEUE)
	void consume(final SmsQueueMessage smsQueueMessage) {
		LOG.info("Stub consumed SMS for recipient {}", smsQueueMessage.recipientId());
		received.add(smsQueueMessage);

		if (silent) {
			// Models messaging still working through its retry ladder: nothing has come back yet.
			return;
		}

		publishOutcome(smsQueueMessage.recipientId(), failingMobileNumbers.contains(smsQueueMessage.mobileNumber()));
	}

	@RabbitListener(queues = EMAIL_WORK_QUEUE)
	void consumeEmail(final EmailQueueMessage emailQueueMessage) {
		LOG.info("Stub consumed e-mail for recipient {}", emailQueueMessage.recipientId());
		receivedEmails.add(emailQueueMessage);

		if (silent) {
			return;
		}

		publishEmailOutcome(emailQueueMessage.recipientId(), failingEmailAddresses.contains(emailQueueMessage.emailAddress()));
	}

	@RabbitListener(queues = DIGITAL_MAIL_WORK_QUEUE)
	void consumeDigitalMail(final DigitalMailQueueMessage digitalMailQueueMessage) {
		LOG.info("Stub consumed digital mail for recipient {}", digitalMailQueueMessage.recipientId());
		receivedDigitalMail.add(digitalMailQueueMessage);

		if (silent) {
			return;
		}

		publishDigitalMailOutcome(digitalMailQueueMessage.recipientId(), failingPartyIds.contains(digitalMailQueueMessage.partyId()));
	}

	@RabbitListener(queues = SNAIL_MAIL_WORK_QUEUE)
	void consumeSnailMail(final SnailMailQueueMessage snailMailQueueMessage) {
		LOG.info("Stub consumed snail mail for recipient {}", snailMailQueueMessage.recipientId());
		receivedSnailMail.add(snailMailQueueMessage);

		if (silent) {
			return;
		}

		publishSnailMailOutcome(snailMailQueueMessage.recipientId(), failingPartyIds.contains(snailMailQueueMessage.partyId()));
	}

	/**
	 * Publishes one terminal outcome. Exposed so a test can send the same outcome twice, which is what a crash between
	 * messaging's publish and its ack looks like from this side.
	 */
	public void publishOutcome(final String recipientId, final boolean failed) {
		publish(recipientId, failed, SENT_ROUTING_KEY, FAILED_ROUTING_KEY, "Invalid mobile number", SmsStatusMessage::new);
	}

	public void publishEmailOutcome(final String recipientId, final boolean failed) {
		publish(recipientId, failed, EMAIL_SENT_ROUTING_KEY, EMAIL_FAILED_ROUTING_KEY, "Invalid e-mail address", EmailStatusMessage::new);
	}

	public void publishDigitalMailOutcome(final String recipientId, final boolean failed) {
		publish(recipientId, failed, DIGITAL_MAIL_SENT_ROUTING_KEY, DIGITAL_MAIL_FAILED_ROUTING_KEY, "No mailbox", DigitalMailStatusMessage::new);
	}

	public void publishSnailMailOutcome(final String recipientId, final boolean failed) {
		publish(recipientId, failed, SNAIL_MAIL_SENT_ROUTING_KEY, SNAIL_MAIL_FAILED_ROUTING_KEY, "Incomplete address", SnailMailStatusMessage::new);
	}

	private void publish(final String recipientId, final boolean failed, final String sentRoutingKey, final String failedRoutingKey,
		final String failureDetail, final StatusMessageFactory factory) {

		if (failed) {
			rabbitTemplate.convertAndSend(STATUS_EXCHANGE, failedRoutingKey, factory.create(recipientId, FAILED, null, failureDetail));
			return;
		}
		rabbitTemplate.convertAndSend(STATUS_EXCHANGE, sentRoutingKey, factory.create(recipientId, SENT, UUID.randomUUID().toString(), null));
	}

	public void reset() {
		received.clear();
		receivedEmails.clear();
		receivedDigitalMail.clear();
		receivedSnailMail.clear();
		failingMobileNumbers = Set.of();
		failingEmailAddresses = Set.of();
		failingPartyIds = Set.of();
		silent = false;
	}

	public void failFor(final String... mobileNumbers) {
		failingMobileNumbers = Set.of(mobileNumbers);
	}

	public void failForEmail(final String... emailAddresses) {
		failingEmailAddresses = Set.of(emailAddresses);
	}

	public void failForPartyId(final String... partyIds) {
		failingPartyIds = Set.of(partyIds);
	}

	public void stayQuiet() {
		silent = true;
	}

	public List<SmsQueueMessage> getReceived() {
		return List.copyOf(received);
	}

	public List<EmailQueueMessage> getReceivedEmails() {
		return List.copyOf(receivedEmails);
	}

	public List<DigitalMailQueueMessage> getReceivedDigitalMail() {
		return List.copyOf(receivedDigitalMail);
	}

	public List<SnailMailQueueMessage> getReceivedSnailMail() {
		return List.copyOf(receivedSnailMail);
	}
}
