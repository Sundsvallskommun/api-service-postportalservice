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
import static se.sundsvall.postportalservice.Constants.SENT;

/**
 * Drives a callback e-mail over a real broker, which is the one path that both publishes onto a queue and uploads
 * attachments to the object store.
 * <p>
 * The assertion that earns this test its keep is the upload count. Delivery runs one task per recipient against a
 * single shared message, so an upload on that path runs recipients &times; attachments times - three recipients and
 * two attachments would be six uploads of the same bytes, and six objects left behind, where two will do.
 */
@Import(RabbitTestTopologyConfiguration.class)
@WireMockAppTestSuite(files = "classpath:/MessageCallbackEmailIT/", classes = Application.class)
class MessageCallbackEmailIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String IDENTIFIER = "joe01doe; type=adAccount";
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
	void test01_successfully_sendCallbackEmailOverQueue() throws FileNotFoundException {
		final var appTest = setupCall();
		final var location = appTest
			.withServicePath("/%s/messages/letter".formatted(MUNICIPALITY_ID))
			.withHttpMethod(POST)
			.withHeader(Identifier.HEADER_NAME, IDENTIFIER)
			.withContentType(MULTIPART_FORM_DATA)
			.withRequestFile("request", REQUEST_FILE)
			.withRequestFile("attachments", "first.pdf")
			.withRequestFile("attachments", "second.pdf")
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("/%s/history/users/joe01doe/messages/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}".formatted(MUNICIPALITY_ID)))
			.withExpectedResponseBodyIsNull()
			.sendRequest()
			.getResponseHeaders()
			.getFirst(LOCATION);

		final var messageId = location.substring(location.lastIndexOf("/") + 1);

		await().atMost(Duration.ofSeconds(15))
			.pollInterval(Duration.ofMillis(100))
			.untilAsserted(() -> {
				final var messageEntity = messageRepository.findById(messageId).orElseThrow();
				assertThat(messageEntity.getRecipients()).hasSize(3);
				assertThat(messageEntity.getRecipients()).allSatisfy(recipient -> {
					assertThat(recipient.getStatus()).isEqualTo(SENT);
					assertThat(recipient.getExternalId()).isNotNull();
				});
			});

		// Two attachments, three recipients: two uploads. Six would mean the upload is back on the per-recipient path.
		verify(exactly(2), putRequestedFor(urlPathMatching(OBJECT_PATH_PATTERN)));

		final var published = messagingQueueStub.getReceivedEmails();
		assertThat(published).hasSize(3);
		assertThat(published).allSatisfy(emailQueueMessage -> {
			assertThat(emailQueueMessage.municipalityId()).isEqualTo(MUNICIPALITY_ID);
			assertThat(emailQueueMessage.messageId()).isEqualTo(messageId);
			assertThat(emailQueueMessage.emailAddress()).isEqualTo("callback@test.sundsvall.se");
			assertThat(emailQueueMessage.sentBy()).isEqualTo(IDENTIFIER);
			// Only the reference travels - the bytes stopped being copied into the message.
			assertThat(emailQueueMessage.attachments()).hasSize(2);
			assertThat(emailQueueMessage.attachments()).allSatisfy(attachment -> assertThat(attachment.objectId()).isNotBlank());
		});

		// Every recipient was handed the same two object ids, which is the same statement the upload count makes,
		// asserted from the payload rather than from the wire.
		final var firstRecipientsObjectIds = published.getFirst().attachments().stream().map(attachment -> attachment.objectId()).toList();
		assertThat(published).allSatisfy(emailQueueMessage -> assertThat(emailQueueMessage.attachments().stream().map(attachment -> attachment.objectId()).toList())
			.isEqualTo(firstRecipientsObjectIds));
		assertThat(firstRecipientsObjectIds).doesNotHaveDuplicates();

		appTest.verifyAllStubs();
	}
}
