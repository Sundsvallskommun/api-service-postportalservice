package se.sundsvall.postportalservice.apptest;

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

import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static se.sundsvall.postportalservice.Constants.FAILED;
import static se.sundsvall.postportalservice.Constants.PENDING;
import static se.sundsvall.postportalservice.Constants.SENT;

/**
 * Drives the two letter channels over a real broker: the service publishes onto their work queues,
 * {@link MessagingQueueStub} stands in for the messaging service and answers on the status queues, and the recipient
 * rows are asserted on the far side of that round trip.
 * <p>
 * {@code MessageLetterIT} covers the same endpoint with the flag off, against WireMock. That is not a leftover - it is
 * the rollback path, and it has to keep passing unchanged for the flag to be worth having.
 */
@Import(RabbitTestTopologyConfiguration.class)
@WireMockAppTestSuite(files = "classpath:/MessageLetterQueueIT/", classes = Application.class)
class MessageLetterQueueIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String IDENTIFIER = "joe01doe; type=adAccount";
	private static final String PARTY_ID = "6d0773d6-3e7f-4552-81bc-f0007af95adf";
	private static final String SNAIL_MAIL_PARTY_ID = "7a1234bc-56de-7890-1234-56789abcdef0";
	private static final String LOCATION_PATTERN =
		"/%s/history/users/joe01doe/messages/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}".formatted(MUNICIPALITY_ID);
	private static final String OBJECT_PATH_PATTERN = "/object-store/objects/messaging-attachments/[0-9a-f-]{36}";

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
	void test01_successfully_sendDigitalMailOverQueue() throws FileNotFoundException {
		final var messageId = sendLetter();

		await().atMost(Duration.ofSeconds(15))
			.pollInterval(Duration.ofMillis(100))
			.untilAsserted(() -> {
				final var messageEntity = messageRepository.findById(messageId).orElseThrow();
				assertThat(messageEntity.getRecipients()).hasSize(1);
				assertThat(messageEntity.getRecipients()).allSatisfy(recipient -> {
					assertThat(recipient.getStatus()).isEqualTo(SENT);
					// The externalId comes back on the status queue, not from the publish.
					assertThat(recipient.getExternalId()).isNotNull();
				});
			});

		// One attachment, one recipient. The count is asserted because the queue path is what introduced uploads on
		// this channel at all - the REST call carried the bytes inline and stored nothing.
		verify(exactly(1), putRequestedFor(urlPathMatching(OBJECT_PATH_PATTERN)));

		final var recipientId = messageRepository.findById(messageId).orElseThrow().getRecipients().getFirst().getId();
		final var published = messagingQueueStub.getReceivedDigitalMail();

		assertThat(published).hasSize(1);
		assertThat(published.getFirst()).satisfies(queueMessage -> {
			assertThat(queueMessage.municipalityId()).isEqualTo(MUNICIPALITY_ID);
			assertThat(queueMessage.messageId()).isEqualTo(messageId);
			assertThat(queueMessage.recipientId()).isEqualTo(recipientId);
			assertThat(queueMessage.partyId()).isEqualTo(PARTY_ID);
			// The mailbox owner messaging sends on behalf of, which is a path parameter on the REST call this replaces.
			assertThat(queueMessage.organizationNumber()).isEqualTo("1234567890");
			assertThat(queueMessage.subject()).isEqualTo("This is the subject of the letter");
			assertThat(queueMessage.body()).isEqualTo("This is the body of the letter");
			assertThat(queueMessage.contentType()).isEqualTo("text/plain");
			assertThat(queueMessage.supportText()).isEqualTo("support text");
			assertThat(queueMessage.sentBy()).isEqualTo(IDENTIFIER);
			assertThat(queueMessage.origin()).isEqualTo("PostPortalService");
			// Only the reference travels: a quorum queue is a poor place for a PDF.
			assertThat(queueMessage.attachments()).singleElement()
				.satisfies(attachment -> assertThat(attachment.objectId()).isNotBlank());
		});
		verifyAllStubs();
	}

	@Test
	void test02_successfully_sendSnailMailOverQueue() throws FileNotFoundException {
		final var messageId = sendLetter();

		await().atMost(Duration.ofSeconds(15))
			.pollInterval(Duration.ofMillis(100))
			.untilAsserted(() -> {
				final var messageEntity = messageRepository.findById(messageId).orElseThrow();
				assertThat(messageEntity.getRecipients()).hasSize(1);
				assertThat(messageEntity.getRecipients()).allSatisfy(recipient -> {
					assertThat(recipient.getStatus()).isEqualTo(SENT);
					assertThat(recipient.getExternalId()).isNotNull();
				});
			});

		verify(exactly(1), putRequestedFor(urlPathMatching(OBJECT_PATH_PATTERN)));

		final var published = messagingQueueStub.getReceivedSnailMail();

		assertThat(published).hasSize(1);
		assertThat(published.getFirst()).satisfies(queueMessage -> {
			assertThat(queueMessage.municipalityId()).isEqualTo(MUNICIPALITY_ID);
			assertThat(queueMessage.messageId()).isEqualTo(messageId);
			// snailmail-sender groups a letter's recipients by the batch id and posts the group as one job. It is the
			// message id here exactly as it is on the REST call, so the grouping does not change with the transport.
			assertThat(queueMessage.batchId()).isEqualTo(messageId);
			assertThat(queueMessage.partyId()).isEqualTo(SNAIL_MAIL_PARTY_ID);
			assertThat(queueMessage.folderName()).isEqualTo("sundsvall");
			assertThat(queueMessage.sentBy()).isEqualTo(IDENTIFIER);
			assertThat(queueMessage.address().zipCode()).isEqualTo("12345");
			assertThat(queueMessage.address().careOf()).isEqualTo("c/o Jane Doe");
			assertThat(queueMessage.attachments()).singleElement()
				.satisfies(attachment -> assertThat(attachment.objectId()).isNotBlank());
		});
		verifyAllStubs();
	}

	@Test
	void test03_letterStaysPendingUntilStatusArrives() throws FileNotFoundException {
		// Models messaging still working through its retry ladder: the letter is on the queue, nothing has come back.
		// This is the behaviour change the flag introduces - before it, DIGITAL_MAIL resolved inside deliver().
		messagingQueueStub.stayQuiet();

		final var messageId = sendLetter();

		await().atMost(Duration.ofSeconds(15))
			.pollInterval(Duration.ofMillis(100))
			.untilAsserted(() -> assertThat(messagingQueueStub.getReceivedDigitalMail()).hasSize(1));

		final var messageEntity = messageRepository.findById(messageId).orElseThrow();
		assertThat(messageEntity.getRecipients()).allSatisfy(recipient -> {
			assertThat(recipient.getStatus()).isEqualTo(PENDING);
			assertThat(recipient.getExternalId()).isNull();
		});
		verifyAllStubs();
	}

	@Test
	void test04_failedOutcomeMarksTheRecipientFailed() throws FileNotFoundException {
		messagingQueueStub.failForPartyId(PARTY_ID);

		final var messageId = sendLetter();

		await().atMost(Duration.ofSeconds(15))
			.pollInterval(Duration.ofMillis(100))
			.untilAsserted(() -> {
				final var messageEntity = messageRepository.findById(messageId).orElseThrow();
				assertThat(messageEntity.getRecipients()).allSatisfy(recipient -> {
					assertThat(recipient.getStatus()).isEqualTo(FAILED);
					assertThat(recipient.getStatusDetail()).isEqualTo("No mailbox");
					assertThat(recipient.getExternalId()).isNull();
				});
			});
		verifyAllStubs();
	}

	@Test
	void test05_duplicateOutcomeDoesNotRevise() throws FileNotFoundException {
		final var messageId = sendLetter();

		await().atMost(Duration.ofSeconds(15))
			.pollInterval(Duration.ofMillis(100))
			.untilAsserted(() -> assertThat(messageRepository.findById(messageId).orElseThrow().getRecipients())
				.allSatisfy(recipient -> assertThat(recipient.getStatus()).isEqualTo(SENT)));

		final var recipient = messageRepository.findById(messageId).orElseThrow().getRecipients().getFirst();
		final var externalId = recipient.getExternalId();

		// messaging publishes the outcome, confirms, then acks, so a crash in between duplicates it. Routed on
		// digital-mail.failed, which also proves the digital-mail.* binding carries both outcome keys onto the queue.
		messagingQueueStub.publishDigitalMailOutcome(recipient.getId(), true);

		await().during(Duration.ofSeconds(2))
			.atMost(Duration.ofSeconds(8))
			.untilAsserted(() -> assertThat(messageRepository.findById(messageId).orElseThrow().getRecipients())
				.allSatisfy(unchanged -> {
					assertThat(unchanged.getStatus()).isEqualTo(SENT);
					assertThat(unchanged.getExternalId()).isEqualTo(externalId);
				}));
		verifyAllStubs();
	}

	private String sendLetter() throws FileNotFoundException {
		final var location = setupCall()
			.withServicePath("/%s/messages/letter".formatted(MUNICIPALITY_ID))
			.withHttpMethod(POST)
			.withHeader(Identifier.HEADER_NAME, IDENTIFIER)
			.withContentType(MULTIPART_FORM_DATA)
			.withRequestFile("request", REQUEST_FILE)
			.withRequestFile("attachments", "test.pdf")
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(LOCATION_PATTERN))
			.withExpectedResponseBodyIsNull()
			.sendRequest()
			.getResponseHeaders()
			.getFirst(LOCATION);

		return location.substring(location.lastIndexOf("/") + 1);
	}
}
