package se.sundsvall.postportalservice.apptest.rabbitmq;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static se.sundsvall.postportalservice.integration.rabbitmq.model.Constants.DIGITAL_REGISTERED_LETTER_EXCHANGE;
import static se.sundsvall.postportalservice.integration.rabbitmq.model.Constants.SEND_DIGITAL_REGISTERED_LETTER_QUEUE;
import static se.sundsvall.postportalservice.integration.rabbitmq.model.Constants.STATUS_DIGITAL_REGISTERED_LETTER_QUEUE;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import se.sundsvall.postportalservice.Application;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.db.dao.RecipientRepository;
import se.sundsvall.postportalservice.integration.rabbitmq.Publisher;
import se.sundsvall.postportalservice.integration.rabbitmq.model.DigitalRegisteredLetterStatusEvent;
import se.sundsvall.postportalservice.integration.rabbitmq.model.Queue;
import se.sundsvall.postportalservice.integration.rabbitmq.model.SendRegisteredLetterEvent;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@ContextConfiguration(initializers = RabbitMQContainerInitializer.class)
class RabbitMQIT {

	@Autowired
	private Publisher publisher;

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private RecipientRepository recipientRepository;

	@Test
	void publishSendRegisteredLetterEvent() {
		final var event = new SendRegisteredLetterEvent(
			"2281",
			"test-request-id",
			"recipient-id",
			new SendRegisteredLetterEvent.Sender("joe01doe", "123456789", "Test Organization", "Support text", "https://example.com", "support@example.com", "0701234567"),
			new SendRegisteredLetterEvent.Recipient("party-id-123"),
			new SendRegisteredLetterEvent.Message("Test Subject", "Test Body", "text/plain", List.of("attachment-id-1", "attachment-id-2")));

		publisher.publishEvent(Queue.SEND_REGISTERED_LETTER, event);

		final var received = rabbitTemplate.receiveAndConvert(SEND_DIGITAL_REGISTERED_LETTER_QUEUE, 5000);

		assertThat(received).isNotNull().isInstanceOf(SendRegisteredLetterEvent.class);
		final var receivedEvent = (SendRegisteredLetterEvent) received;
		assertThat(receivedEvent.municipalityId()).isEqualTo("2281");
		assertThat(receivedEvent.requestId()).isEqualTo("test-request-id");
		assertThat(receivedEvent.recipientId()).isEqualTo("recipient-id");
		assertThat(receivedEvent.sender().identifier()).isEqualTo("joe01doe");
		assertThat(receivedEvent.sender().organizationNumber()).isEqualTo("123456789");
		assertThat(receivedEvent.sender().organizationName()).isEqualTo("Test Organization");
		assertThat(receivedEvent.sender().supportText()).isEqualTo("Support text");
		assertThat(receivedEvent.sender().contactInformationUrl()).isEqualTo("https://example.com");
		assertThat(receivedEvent.sender().contactInformationEmail()).isEqualTo("support@example.com");
		assertThat(receivedEvent.sender().contactInformationPhoneNumber()).isEqualTo("0701234567");
		assertThat(receivedEvent.recipient().partyId()).isEqualTo("party-id-123");
		assertThat(receivedEvent.message().subject()).isEqualTo("Test Subject");
		assertThat(receivedEvent.message().body()).isEqualTo("Test Body");
		assertThat(receivedEvent.message().contentType()).isEqualTo("text/plain");
		assertThat(receivedEvent.message().attachmentIds()).containsExactly("attachment-id-1", "attachment-id-2");
	}

	@Test
	void consumeStatusEvent() {
		final var recipient = recipientRepository.save(RecipientEntity.create()
			.withPartyId("party-id-456")
			.withStatus("PENDING"));

		final var statusEvent = new DigitalRegisteredLetterStatusEvent(
			recipient.getId(),
			"external-id-from-kivra",
			"SENT",
			null);

		rabbitTemplate.convertAndSend(DIGITAL_REGISTERED_LETTER_EXCHANGE, STATUS_DIGITAL_REGISTERED_LETTER_QUEUE, statusEvent);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			final var updated = recipientRepository.findById(recipient.getId()).orElseThrow();
			assertThat(updated.getStatus()).isEqualTo("SENT");
			assertThat(updated.getExternalId()).isEqualTo("external-id-from-kivra");
			assertThat(updated.getStatusDetail()).isNull();
		});
	}

	@Test
	void consumeStatusEventWithStatusDetail() {
		final var recipient = recipientRepository.save(RecipientEntity.create()
			.withPartyId("party-id-789")
			.withStatus("PENDING"));

		final var statusEvent = new DigitalRegisteredLetterStatusEvent(
			recipient.getId(),
			"external-id-failed",
			"FAILED",
			"Recipient does not have a digital mailbox");

		rabbitTemplate.convertAndSend(DIGITAL_REGISTERED_LETTER_EXCHANGE, STATUS_DIGITAL_REGISTERED_LETTER_QUEUE, statusEvent);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			final var updated = recipientRepository.findById(recipient.getId()).orElseThrow();
			assertThat(updated.getStatus()).isEqualTo("FAILED");
			assertThat(updated.getExternalId()).isEqualTo("external-id-failed");
			assertThat(updated.getStatusDetail()).isEqualTo("Recipient does not have a digital mailbox");
		});
	}
}
