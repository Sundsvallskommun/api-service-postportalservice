package se.sundsvall.postportalservice.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static se.sundsvall.postportalservice.Constants.FAILED;
import static se.sundsvall.postportalservice.Constants.PENDING;
import static se.sundsvall.postportalservice.Constants.SENT;
import static se.sundsvall.postportalservice.integration.db.converter.MessageType.SMS;

import java.io.FileNotFoundException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.postportalservice.Application;
import se.sundsvall.postportalservice.apptest.support.MessagingQueueStub;
import se.sundsvall.postportalservice.apptest.support.RabbitTestTopologyConfiguration;
import se.sundsvall.postportalservice.integration.db.dao.MessageRepository;

/**
 * Drives SMS over a real broker: the service publishes onto the work queue, {@link MessagingQueueStub} stands in for
 * the messaging service and answers on the status queue, and the recipient rows are asserted on the far side of that
 * round trip.
 */
@Import(RabbitTestTopologyConfiguration.class)
@WireMockAppTestSuite(files = "classpath:/MessageSmsIT/", classes = Application.class)
class MessageSmsIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String IDENTIFIER = "joe01doe; type=adAccount";

	// Singleton container: started once for the class and torn down by Ryuk, so the JUnit extension is not needed.
	static final RabbitMQContainer RABBIT_MQ = new RabbitMQContainer("rabbitmq:4-management-alpine");

	static {
		RABBIT_MQ.start();
	}

	@DynamicPropertySource
	static void rabbitProperties(final DynamicPropertyRegistry registry) {
		registry.add("spring.rabbitmq.host", RABBIT_MQ::getHost);
		registry.add("spring.rabbitmq.port", RABBIT_MQ::getAmqpPort);
		registry.add("spring.rabbitmq.username", RABBIT_MQ::getAdminUsername);
		registry.add("spring.rabbitmq.password", RABBIT_MQ::getAdminPassword);
		registry.add("rabbitmq.enabled", () -> "true");
	}

	@Autowired
	private MessageRepository messageRepository;

	@Autowired
	private MessagingQueueStub messagingQueueStub;

	@BeforeEach
	void resetStub() {
		messagingQueueStub.reset();
	}

	@Test
	void test01_successfully_sendSingleSms() {
		final var appTest = setupCall();
		final var location = appTest
			.withServicePath("/%s/messages/sms".formatted(MUNICIPALITY_ID))
			.withHttpMethod(POST)
			.withHeader(Identifier.HEADER_NAME, IDENTIFIER)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("/%s/history/users/joe01doe/messages/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}".formatted(MUNICIPALITY_ID)))
			.withExpectedResponseBodyIsNull()
			.sendRequest()
			.getResponseHeaders()
			.getFirst(LOCATION);

		final var messageId = location.substring(location.lastIndexOf("/") + 1);

		// There are asynchronous processes involved in updating the recipient status, hence we need to wait until the expected state is reached
		await().atMost(Duration.ofSeconds(10))
			.pollInterval(Duration.ofMillis(100))
			.untilAsserted(() -> {
				var message = messageRepository.findById(messageId).orElseThrow();
				assertThat(message.getRecipients()).hasSize(1);
				assertThat(message.getRecipients())
					.allSatisfy(r -> assertThat(r.getStatus()).isEqualTo(SENT));
				// The externalId comes back on the status queue, not from the publish.
				assertThat(message.getRecipients())
					.allSatisfy(r -> assertThat(r.getExternalId()).isNotNull());
			});

		final var recipientId = messageRepository.findById(messageId).orElseThrow().getRecipients().getFirst().getId();
		final var published = messagingQueueStub.getReceived();
		assertThat(published).hasSize(1);
		assertThat(published.getFirst()).satisfies(smsQueueMessage -> {
			assertThat(smsQueueMessage.municipalityId()).isEqualTo(MUNICIPALITY_ID);
			assertThat(smsQueueMessage.messageId()).isEqualTo(messageId);
			assertThat(smsQueueMessage.mobileNumber()).isEqualTo("+46701740605");
			assertThat(smsQueueMessage.partyId()).isEqualTo("6d0773d6-3e7f-4552-81bc-f0007af95adf");
			assertThat(smsQueueMessage.message()).isEqualTo("This is the message to be sent");
			assertThat(smsQueueMessage.sentBy()).isEqualTo(IDENTIFIER);
			assertThat(smsQueueMessage.origin()).isEqualTo("PostPortalService");
			// The idempotency key messaging is expected to pass on to sms-sender.
			assertThat(smsQueueMessage.recipientId()).isEqualTo(recipientId);
		});
		appTest.verifyAllStubs();
	}

	@Test
	void test02_successfully_sendMultipleSms() {
		final var appTest = setupCall();
		final var location = appTest
			.withServicePath("/%s/messages/sms".formatted(MUNICIPALITY_ID))
			.withHttpMethod(POST)
			.withHeader(Identifier.HEADER_NAME, IDENTIFIER)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("/%s/history/users/joe01doe/messages/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}".formatted(MUNICIPALITY_ID)))
			.withExpectedResponseBodyIsNull()
			.sendRequest()
			.getResponseHeaders()
			.getFirst(LOCATION);

		final var messageId = location.substring(location.lastIndexOf("/") + 1);

		// There are asynchronous processes involved in updating the recipient status, hence we need to wait until the expected state is reached
		await().atMost(Duration.ofSeconds(10))
			.pollInterval(Duration.ofMillis(100))
			.untilAsserted(() -> {
				final var messageEntity = messageRepository.findById(messageId).orElseThrow();
				assertThat(messageEntity.getRecipients()).hasSize(3);
				assertThat(messageEntity.getRecipients()).allSatisfy(recipient -> {
					assertThat(recipient.getStatus()).isEqualTo(SENT);
					assertThat(recipient.getMessageType()).isEqualTo(SMS);
				});
			});
		appTest.verifyAllStubs();
	}

	@Test
	void test03_unsuccessfully_sendSingleSms() {
		messagingQueueStub.failFor("+46701740605");

		final var appTest = setupCall();
		final var location = appTest
			.withServicePath("/%s/messages/sms".formatted(MUNICIPALITY_ID))
			.withHttpMethod(POST)
			.withHeader(Identifier.HEADER_NAME, IDENTIFIER)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("/%s/history/users/joe01doe/messages/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}".formatted(MUNICIPALITY_ID)))
			.withExpectedResponseBodyIsNull()
			.sendRequest()
			.getResponseHeaders()
			.getFirst(LOCATION);

		final var messageId = location.substring(location.lastIndexOf("/") + 1);

		// There are asynchronous processes involved in updating the recipient status, hence we need to wait until the expected state is reached
		await().atMost(Duration.ofSeconds(10))
			.pollInterval(Duration.ofMillis(100))
			.untilAsserted(() -> {
				final var messageEntity = messageRepository.findById(messageId).orElseThrow();
				assertThat(messageEntity.getRecipients()).hasSize(1);
				assertThat(messageEntity.getRecipients()).allSatisfy(recipient -> {
					assertThat(recipient.getStatus()).isEqualTo(FAILED);
					assertThat(recipient.getMessageType()).isEqualTo(SMS);
				});
			});
		appTest.verifyAllStubs();
	}

	@Test
	void test04_unsuccessfully_sendMultipleSms() {
		messagingQueueStub.failFor("+46701740605", "+46701740606", "+46701740607");

		final var appTest = setupCall();
		final var location = appTest
			.withServicePath("/%s/messages/sms".formatted(MUNICIPALITY_ID))
			.withHttpMethod(POST)
			.withHeader(Identifier.HEADER_NAME, IDENTIFIER)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("/%s/history/users/joe01doe/messages/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}".formatted(MUNICIPALITY_ID)))
			.withExpectedResponseBodyIsNull()
			.sendRequest()
			.getResponseHeaders()
			.getFirst(LOCATION);

		final var messageId = location.substring(location.lastIndexOf("/") + 1);

		// There are asynchronous processes involved in updating the recipient status, hence we need to wait until the expected state is reached
		await().atMost(Duration.ofSeconds(10))
			.pollInterval(Duration.ofMillis(100))
			.untilAsserted(() -> {
				final var messageEntity = messageRepository.findById(messageId).orElseThrow();
				assertThat(messageEntity.getRecipients()).hasSize(3);
				assertThat(messageEntity.getRecipients()).allSatisfy(recipient -> {
					assertThat(recipient.getStatus()).isEqualTo(FAILED);
					assertThat(recipient.getMessageType()).isEqualTo(SMS);
				});
			});
		appTest.verifyAllStubs();
	}

	@Test
	void test05_partially_successful_sendMultipleSms() {
		messagingQueueStub.failFor("+46701740606", "+46701740607");

		final var appTest = setupCall();
		final var location = appTest
			.withServicePath("/%s/messages/sms".formatted(MUNICIPALITY_ID))
			.withHttpMethod(POST)
			.withHeader(Identifier.HEADER_NAME, IDENTIFIER)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("/%s/history/users/joe01doe/messages/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}".formatted(MUNICIPALITY_ID)))
			.withExpectedResponseBodyIsNull()
			.sendRequest()
			.getResponseHeaders()
			.getFirst(LOCATION);

		final var messageId = location.substring(location.lastIndexOf("/") + 1);

		// There are asynchronous processes involved in updating the recipient status, hence we need to wait until the expected state is reached
		await().atMost(Duration.ofSeconds(10))
			.pollInterval(Duration.ofMillis(100))
			.untilAsserted(() -> {
				final var messageEntity = messageRepository.findById(messageId).orElseThrow();
				assertThat(messageEntity.getRecipients()).hasSize(3);
				assertThat(messageEntity.getRecipients())
					.filteredOn(recipient -> recipient.getStatus().equals(SENT))
					.hasSize(1);
				assertThat(messageEntity.getRecipients())
					.filteredOn(recipient -> recipient.getStatus().equals(FAILED))
					.hasSize(2);
			});
		appTest.verifyAllStubs();
	}

	@Test
	void test06_successfully_sendSmsCsv() throws FileNotFoundException {
		final var appTest = setupCall();
		final var location = appTest
			.withServicePath("/%s/messages/sms/csv".formatted(MUNICIPALITY_ID))
			.withHttpMethod(POST)
			.withHeader(Identifier.HEADER_NAME, IDENTIFIER)
			.withContentType(MULTIPART_FORM_DATA)
			.withRequestFile("request", REQUEST_FILE)
			.withRequestFile("csv-file", "phones.csv")
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("/%s/history/users/joe01doe/messages/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}".formatted(MUNICIPALITY_ID)))
			.withExpectedResponseBodyIsNull()
			.sendRequest()
			.getResponseHeaders()
			.getFirst(LOCATION);

		final var messageId = location.substring(location.lastIndexOf("/") + 1);

		// There are asynchronous processes involved in updating the recipient status, hence we need to wait until the expected state is reached
		await().atMost(Duration.ofSeconds(10))
			.pollInterval(Duration.ofMillis(100))
			.untilAsserted(() -> {
				final var messageEntity = messageRepository.findById(messageId).orElseThrow();
				assertThat(messageEntity.getRecipients()).hasSize(3);
				assertThat(messageEntity.getRecipients()).allSatisfy(recipient -> {
					assertThat(recipient.getStatus()).isEqualTo(SENT);
					assertThat(recipient.getMessageType()).isEqualTo(SMS);
				});
			});
		appTest.verifyAllStubs();
	}


	@Test
	void test07_smsStaysPendingUntilStatusArrives() {
		// Models messaging still working through its retry ladder: the SMS is on the queue, nothing has come back.
		messagingQueueStub.stayQuiet();

		final var appTest = setupCall();
		final var location = appTest
			.withServicePath("/%s/messages/sms".formatted(MUNICIPALITY_ID))
			.withHttpMethod(POST)
			.withHeader(Identifier.HEADER_NAME, IDENTIFIER)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("/%s/history/users/joe01doe/messages/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}".formatted(MUNICIPALITY_ID)))
			.withExpectedResponseBodyIsNull()
			.sendRequest()
			.getResponseHeaders()
			.getFirst(LOCATION);

		final var messageId = location.substring(location.lastIndexOf("/") + 1);

		await().atMost(Duration.ofSeconds(10))
			.pollInterval(Duration.ofMillis(100))
			.untilAsserted(() -> assertThat(messagingQueueStub.getReceived()).hasSize(1));

		// The publish itself never decides the outcome, so the recipient is left PENDING until the status arrives.
		final var messageEntity = messageRepository.findById(messageId).orElseThrow();
		assertThat(messageEntity.getRecipients()).hasSize(1);
		assertThat(messageEntity.getRecipients()).allSatisfy(recipient -> {
			assertThat(recipient.getStatus()).isEqualTo(PENDING);
			assertThat(recipient.getExternalId()).isNull();
			assertThat(recipient.getMessageType()).isEqualTo(SMS);
		});
		appTest.verifyAllStubs();
	}


	@Test
	void test08_duplicateOutcomeDoesNotRevise() {
		final var appTest = setupCall();
		final var location = appTest
			.withServicePath("/%s/messages/sms".formatted(MUNICIPALITY_ID))
			.withHttpMethod(POST)
			.withHeader(Identifier.HEADER_NAME, IDENTIFIER)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("/%s/history/users/joe01doe/messages/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}".formatted(MUNICIPALITY_ID)))
			.withExpectedResponseBodyIsNull()
			.sendRequest()
			.getResponseHeaders()
			.getFirst(LOCATION);

		final var messageId = location.substring(location.lastIndexOf("/") + 1);

		await().atMost(Duration.ofSeconds(10))
			.pollInterval(Duration.ofMillis(100))
			.untilAsserted(() -> assertThat(messageRepository.findById(messageId).orElseThrow().getRecipients())
				.allSatisfy(recipient -> assertThat(recipient.getStatus()).isEqualTo(SENT)));

		final var recipient = messageRepository.findById(messageId).orElseThrow().getRecipients().getFirst();
		final var externalId = recipient.getExternalId();

		// messaging publishes the outcome, confirms, then acks, so a crash in between duplicates it. Routed on
		// sms.failed, which also proves the sms.* binding carries both outcome keys onto the queue.
		messagingQueueStub.publishOutcome(recipient.getId(), true);

		await().during(Duration.ofSeconds(2))
			.atMost(Duration.ofSeconds(6))
			.untilAsserted(() -> assertThat(messageRepository.findById(messageId).orElseThrow().getRecipients())
				.allSatisfy(r -> {
					assertThat(r.getStatus()).isEqualTo(SENT);
					assertThat(r.getExternalId()).isEqualTo(externalId);
				}));
		appTest.verifyAllStubs();
	}

}
