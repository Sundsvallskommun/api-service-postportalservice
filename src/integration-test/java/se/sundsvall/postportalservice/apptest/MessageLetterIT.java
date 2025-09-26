package se.sundsvall.postportalservice.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static se.sundsvall.postportalservice.Constants.SENT;
import static se.sundsvall.postportalservice.integration.db.converter.MessageType.DIGITAL_MAIL;
import static se.sundsvall.postportalservice.integration.db.converter.MessageType.SNAIL_MAIL;

import java.io.FileNotFoundException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.postportalservice.Application;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.db.converter.MessageType;
import se.sundsvall.postportalservice.integration.db.dao.MessageRepository;

@WireMockAppTestSuite(files = "classpath:/MessageLetterIT/", classes = Application.class)
class MessageLetterIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String IDENTIFIER = "type=adAccount; TestUser";

	@Autowired
	private MessageRepository messageRepository;

	@Test
	void test01_successfully_sendDigitalMail() throws FileNotFoundException {
		var location = setupCall()
			.withServicePath("/%s/messages/letter".formatted(MUNICIPALITY_ID))
			.withHttpMethod(POST)
			.withHeader(Identifier.HEADER_NAME, IDENTIFIER)
			.withContentType(MULTIPART_FORM_DATA)
			.withRequestFile("request", REQUEST_FILE)
			.withRequestFile("attachments", "test.pdf")
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("/%s/history/messages/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}".formatted(MUNICIPALITY_ID)))
			.withExpectedResponseBodyIsNull()
			.sendRequest()
			.getResponseHeaders()
			.getFirst(LOCATION);

		final var messageId = location.substring(location.lastIndexOf("/") + 1);

		// There are asynchronous processes involved in updating the recipient status, hence we need to wait until the expected state is reached
		await().atMost(Duration.ofSeconds(3))
			.pollInterval(Duration.ofMillis(100))
			.untilAsserted(() -> {
				var message = messageRepository.findById(messageId).orElseThrow();
				assertThat(message.getRecipients()).hasSize(1);
				assertThat(message.getRecipients())
					.allSatisfy(recipientEntity -> {
						assertThat(recipientEntity.getStatus()).isEqualTo(SENT);
						assertThat(recipientEntity.getMessageType()).isEqualTo(DIGITAL_MAIL);
					});
			});
	}

	@Test
	void test02_successfully_sendSnailMail() throws FileNotFoundException {
		var location = setupCall()
			.withServicePath("/%s/messages/letter".formatted(MUNICIPALITY_ID))
			.withHttpMethod(POST)
			.withHeader(Identifier.HEADER_NAME, IDENTIFIER)
			.withContentType(MULTIPART_FORM_DATA)
			.withRequestFile("request", REQUEST_FILE)
			.withRequestFile("attachments", "test.pdf")
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("/%s/history/messages/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}".formatted(MUNICIPALITY_ID)))
			.withExpectedResponseBodyIsNull()
			.sendRequest()
			.getResponseHeaders()
			.getFirst(LOCATION);

		final var messageId = location.substring(location.lastIndexOf("/") + 1);

		// There are asynchronous processes involved in updating the recipient status, hence we need to wait until the expected state is reached
		await().atMost(Duration.ofSeconds(3))
			.pollInterval(Duration.ofMillis(100))
			.untilAsserted(() -> {
				var message = messageRepository.findById(messageId).orElseThrow();
				assertThat(message.getRecipients()).hasSize(1);
				assertThat(message.getRecipients())
					.allSatisfy(recipientEntity -> {
						assertThat(recipientEntity.getStatus()).isEqualTo(SENT);
						assertThat(recipientEntity.getMessageType()).isEqualTo(SNAIL_MAIL);
					});
			});
	}

	@Test
	void test03_successfully_sendSnailMailAndDigitalMail() throws FileNotFoundException {
		var location = setupCall()
			.withServicePath("/%s/messages/letter".formatted(MUNICIPALITY_ID))
			.withHttpMethod(POST)
			.withHeader(Identifier.HEADER_NAME, IDENTIFIER)
			.withContentType(MULTIPART_FORM_DATA)
			.withRequestFile("request", REQUEST_FILE)
			.withRequestFile("attachments", "test.pdf")
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("/%s/history/messages/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}".formatted(MUNICIPALITY_ID)))
			.withExpectedResponseBodyIsNull()
			.sendRequest()
			.getResponseHeaders()
			.getFirst(LOCATION);

		final var messageId = location.substring(location.lastIndexOf("/") + 1);

		// There are asynchronous processes involved in updating the recipient status, hence we need to wait until the expected state is reached
		await().atMost(Duration.ofSeconds(3))
			.pollInterval(Duration.ofMillis(100))
			.untilAsserted(() -> {
				var message = messageRepository.findById(messageId).orElseThrow();
				assertThat(message.getRecipients()).hasSize(2);
				assertThat(message.getRecipients())
					.hasSize(2)
					.extracting(RecipientEntity::getMessageType)
					.containsExactlyInAnyOrder(MessageType.SNAIL_MAIL, DIGITAL_MAIL);
				assertThat(message.getRecipients())
					.allSatisfy(recipientEntity -> assertThat(recipientEntity.getStatus()).isEqualTo(SENT));
			});
	}

	@Test
	void test04_successfully_snailMailToAddress() throws FileNotFoundException {
		var location = setupCall()
			.withServicePath("/%s/messages/letter".formatted(MUNICIPALITY_ID))
			.withHttpMethod(POST)
			.withHeader(Identifier.HEADER_NAME, IDENTIFIER)
			.withContentType(MULTIPART_FORM_DATA)
			.withRequestFile("request", REQUEST_FILE)
			.withRequestFile("attachments", "test.pdf")
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("/%s/history/messages/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}".formatted(MUNICIPALITY_ID)))
			.withExpectedResponseBodyIsNull()
			.sendRequest()
			.getResponseHeaders()
			.getFirst(LOCATION);

		final var messageId = location.substring(location.lastIndexOf("/") + 1);

		// There are asynchronous processes involved in updating the recipient status, hence we need to wait until the expected state is reached
		await().atMost(Duration.ofSeconds(3))
			.pollInterval(Duration.ofMillis(100))
			.untilAsserted(() -> {
				var message = messageRepository.findById(messageId).orElseThrow();
				assertThat(message.getRecipients()).hasSize(1);
				assertThat(message.getRecipients())
					.allSatisfy(recipientEntity -> assertThat(recipientEntity.getStatus()).isEqualTo(SENT));
			});
	}

}
