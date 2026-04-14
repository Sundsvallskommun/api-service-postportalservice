package se.sundsvall.postportalservice.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static se.sundsvall.postportalservice.Constants.PENDING;
import static se.sundsvall.postportalservice.integration.db.converter.MessageType.DIGITAL_REGISTERED_LETTER;
import static se.sundsvall.postportalservice.integration.rabbitmq.model.Constants.SEND_DIGITAL_REGISTERED_LETTER_QUEUE;

import java.io.FileNotFoundException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.postportalservice.Application;
import se.sundsvall.postportalservice.apptest.rabbitmq.RabbitMQContainerInitializer;
import se.sundsvall.postportalservice.integration.db.dao.MessageRepository;
import se.sundsvall.postportalservice.integration.rabbitmq.model.SendRegisteredLetterEvent;

@WireMockAppTestSuite(files = "classpath:/MessageDigitalRegisteredLetterIT/", classes = Application.class)
@ContextConfiguration(initializers = RabbitMQContainerInitializer.class)
class MessageDigitalRegisteredLetterIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String IDENTIFIER = "joe01doe; type=adAccount";

	@Autowired
	private RabbitTemplate rabbitTemplate;

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
			.withExpectedResponseHeader(LOCATION, List.of("/%s/history/users/joe01doe/messages/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}".formatted(MUNICIPALITY_ID)))
			.withExpectedResponseBodyIsNull()
			.sendRequest()
			.getResponseHeaders()
			.getFirst(LOCATION);

		final var messageId = location.substring(location.lastIndexOf("/") + 1);

		// Verify database state
		final var message = messageRepository.findById(messageId).orElseThrow();
		assertThat(message.getRecipients()).hasSize(1);
		assertThat(message.getRecipients().getFirst().getStatus()).isEqualTo(PENDING);
		assertThat(message.getRecipients().getFirst().getMessageType()).isEqualTo(DIGITAL_REGISTERED_LETTER);
		assertThat(message.getRecipients().getFirst().getExternalId()).isNull();

		// Verify the published RabbitMQ event
		final var received = rabbitTemplate.receiveAndConvert(SEND_DIGITAL_REGISTERED_LETTER_QUEUE, 5000);

		assertThat(received).isNotNull().isInstanceOf(SendRegisteredLetterEvent.class);
		final var event = (SendRegisteredLetterEvent) received;
		assertThat(event.municipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(event.requestId()).isNotBlank();
		assertThat(event.recipientId()).isEqualTo(message.getRecipients().getFirst().getId());
		assertThat(event.sender().identifier()).isEqualTo("joe01doe");
		assertThat(event.sender().organizationNumber()).isEqualTo("1234567890");
		assertThat(event.sender().organizationName()).isEqualTo("name");
		assertThat(event.sender().supportText()).isEqualTo("support text");
		assertThat(event.sender().contactInformationUrl()).isEqualTo("http://test.se/test");
		assertThat(event.sender().contactInformationEmail()).isEqualTo("test@test.com");
		assertThat(event.sender().contactInformationPhoneNumber()).isEqualTo("+46123456789321");
		assertThat(event.recipient().partyId()).isEqualTo("6d0773d6-3e7f-4552-81bc-f0007af95adf");
		assertThat(event.message().subject()).isEqualTo("This is the subject of the letter");
		assertThat(event.message().body()).isEqualTo("<h1>This is the body of the letter</h1>");
		assertThat(event.message().contentType()).isEqualTo("text/html");
		assertThat(event.message().attachmentIds()).hasSize(1);
	}
}
