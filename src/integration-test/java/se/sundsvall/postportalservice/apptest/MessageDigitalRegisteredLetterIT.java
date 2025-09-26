package se.sundsvall.postportalservice.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static se.sundsvall.postportalservice.Constants.FAILED;
import static se.sundsvall.postportalservice.Constants.SENT;
import static se.sundsvall.postportalservice.integration.db.converter.MessageType.DIGITAL_REGISTERED_LETTER;

import java.io.FileNotFoundException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.postportalservice.Application;
import se.sundsvall.postportalservice.integration.db.dao.MessageRepository;

@WireMockAppTestSuite(files = "classpath:/MessageDigitalRegisteredLetterIT/", classes = Application.class)
class MessageDigitalRegisteredLetterIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String IDENTIFIER = "type=adAccount; TestUser";

	@Autowired
	private MessageRepository messageRepository;

	@Test
	void test01_successfully_sendDigitalRegisteredLetter() throws FileNotFoundException {
		final var location = setupCall()
			.withServicePath("/%s/messages/registered-letter".formatted(MUNICIPALITY_ID))
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

		final var message = messageRepository.findById(messageId).orElseThrow();
		assertThat(message.getRecipients()).hasSize(1);
		assertThat(message.getRecipients())
			.allSatisfy(recipientEntity -> {
				assertThat(recipientEntity.getStatus()).isEqualTo(SENT);
				assertThat(recipientEntity.getExternalId()).isEqualTo("drl-external-id-123");
				assertThat(recipientEntity.getMessageType()).isEqualTo(DIGITAL_REGISTERED_LETTER);
			});
	}

	@Test
	void test02_unsuccessfully_sendDigitalRegisteredLetter() throws FileNotFoundException {
		final var location = setupCall()
			.withServicePath("/%s/messages/registered-letter".formatted(MUNICIPALITY_ID))
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

		final var message = messageRepository.findById(messageId).orElseThrow();
		assertThat(message.getRecipients()).hasSize(1);
		assertThat(message.getRecipients())
			.allSatisfy(recipientEntity -> {
				assertThat(recipientEntity.getStatus()).isEqualTo(FAILED);
				assertThat(recipientEntity.getExternalId()).isEqualTo("drl-external-id-123");
				assertThat(recipientEntity.getMessageType()).isEqualTo(DIGITAL_REGISTERED_LETTER);
			});
	}

}
