package se.sundsvall.postportalservice.apptest;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.postportalservice.Application;
import se.sundsvall.postportalservice.integration.db.dao.MessageRepository;
import se.sundsvall.postportalservice.integration.db.dao.SigningRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static se.sundsvall.postportalservice.Constants.SENT;
import static se.sundsvall.postportalservice.Constants.SIGNED;
import static se.sundsvall.postportalservice.integration.db.converter.MessageType.DIGITAL_MAIL;

@Sql(scripts = {
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata.sql"
})
@WireMockAppTestSuite(files = "classpath:/MessageSignedDocumentLetterIT/", classes = Application.class)
class MessageSignedDocumentLetterIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String IDENTIFIER = "joe01doe; type=adAccount";
	private static final String SIGNED_MESSAGE_ID = "5e1d0000-0000-4000-8000-000000000001";
	private static final String PENDING_MESSAGE_ID = "b2cd4957-228f-46f0-a263-d4eae2eb5f52";
	private static final String SIGNED_ATTACHMENT_ID = "5e1d0000-0000-4000-8000-0000000000d1";

	@Autowired
	private MessageRepository messageRepository;

	@Autowired
	private SigningRepository signingRepository;

	@Test
	void test01_sendSignedDocumentAsDigitalLetter() {
		final var appTest = setupCall();
		final var location = appTest
			.withServicePath("/%s/messages/e-signing/%s/forward".formatted(MUNICIPALITY_ID, SIGNED_MESSAGE_ID))
			.withHttpMethod(POST)
			.withHeader(Identifier.HEADER_NAME, IDENTIFIER)
			.withContentType(APPLICATION_JSON)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("/%s/history/users/joe01doe/messages/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}".formatted(MUNICIPALITY_ID)))
			.withExpectedResponseBodyIsNull()
			.sendRequest()
			.getResponseHeaders()
			.getFirst(LOCATION);

		final var letterId = location.substring(location.lastIndexOf("/") + 1);

		// There are asynchronous processes involved in updating the recipient status, hence we need to wait until the expected
		// state is reached
		await().atMost(Duration.ofSeconds(3))
			.pollInterval(Duration.ofMillis(100))
			.untilAsserted(() -> {
				final var letter = messageRepository.findById(letterId).orElseThrow();
				assertThat(letter.getRecipients()).hasSize(1);
				assertThat(letter.getRecipients()).allSatisfy(recipient -> {
					assertThat(recipient.getStatus()).isEqualTo(SENT);
					assertThat(recipient.getMessageType()).isEqualTo(DIGITAL_MAIL);
				});
			});

		final var signing = signingRepository.findByMessageId(SIGNED_MESSAGE_ID).orElseThrow();
		assertThat(signing.getStatus()).isEqualTo(SIGNED);
		assertThat(signing.getAttachment().getId()).isEqualTo(SIGNED_ATTACHMENT_ID);

		appTest.verifyAllStubs();
	}

	@Test
	void test02_sendSignedDocumentWhenNotSigned() {
		setupCall()
			.withServicePath("/%s/messages/e-signing/%s/forward".formatted(MUNICIPALITY_ID, PENDING_MESSAGE_ID))
			.withHttpMethod(POST)
			.withHeader(Identifier.HEADER_NAME, IDENTIFIER)
			.withContentType(APPLICATION_JSON)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_sendSignedDocumentWhenSigningDoesNotExist() {
		setupCall()
			.withServicePath("/%s/messages/e-signing/%s/forward".formatted(MUNICIPALITY_ID, "00000000-0000-4000-8000-000000000000"))
			.withHttpMethod(POST)
			.withHeader(Identifier.HEADER_NAME, IDENTIFIER)
			.withContentType(APPLICATION_JSON)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_sendSignedDocumentWhenRecipientHasNoDigitalMailbox() {
		setupCall()
			.withServicePath("/%s/messages/e-signing/%s/forward".formatted(MUNICIPALITY_ID, SIGNED_MESSAGE_ID))
			.withHttpMethod(POST)
			.withHeader(Identifier.HEADER_NAME, IDENTIFIER)
			.withContentType(APPLICATION_JSON)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}
}
