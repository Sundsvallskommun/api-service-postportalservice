package se.sundsvall.postportalservice.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.CREATED;
import static se.sundsvall.postportalservice.Constants.FAILED;
import static se.sundsvall.postportalservice.Constants.SENT;
import static se.sundsvall.postportalservice.integration.db.converter.MessageType.SMS;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.postportalservice.Application;
import se.sundsvall.postportalservice.integration.db.dao.MessageRepository;

@WireMockAppTestSuite(files = "classpath:/MessageSmsIT/", classes = Application.class)
class MessageSmsIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String IDENTIFIER = "type=adAccount; TestUser";

	@Autowired
	private MessageRepository messageRepository;

	@Test
	void test01_successfully_sendSingleSms() {
		final var location = setupCall()
			.withServicePath("/%s/messages/sms".formatted(MUNICIPALITY_ID))
			.withHttpMethod(POST)
			.withHeader(Identifier.HEADER_NAME, IDENTIFIER)
			.withRequest(REQUEST_FILE)
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
					.allSatisfy(r -> assertThat(r.getStatus()).isEqualTo(SENT));
			});

	}

	@Test
	void test02_successfully_sendMultipleSms() {
		final var location = setupCall()
			.withServicePath("/%s/messages/sms".formatted(MUNICIPALITY_ID))
			.withHttpMethod(POST)
			.withHeader(Identifier.HEADER_NAME, IDENTIFIER)
			.withRequest(REQUEST_FILE)
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
				final var messageEntity = messageRepository.findById(messageId).orElseThrow();
				assertThat(messageEntity.getRecipients()).hasSize(3);
				assertThat(messageEntity.getRecipients()).allSatisfy(recipient -> {
					assertThat(recipient.getStatus()).isEqualTo(SENT);
					assertThat(recipient.getMessageType()).isEqualTo(SMS);
				});
			});

	}

	@Test
	void test03_unsuccessfully_sendSingleSms() {
		final var location = setupCall()
			.withServicePath("/%s/messages/sms".formatted(MUNICIPALITY_ID))
			.withHttpMethod(POST)
			.withHeader(Identifier.HEADER_NAME, IDENTIFIER)
			.withRequest(REQUEST_FILE)
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
				final var messageEntity = messageRepository.findById(messageId).orElseThrow();
				assertThat(messageEntity.getRecipients()).hasSize(1);
				assertThat(messageEntity.getRecipients()).allSatisfy(recipient -> {
					assertThat(recipient.getStatus()).isEqualTo(FAILED);
					assertThat(recipient.getMessageType()).isEqualTo(SMS);
				});
			});

	}

	@Test
	void test04_unsuccessfully_sendMultipleSms() {
		final var location = setupCall()
			.withServicePath("/%s/messages/sms".formatted(MUNICIPALITY_ID))
			.withHttpMethod(POST)
			.withHeader(Identifier.HEADER_NAME, IDENTIFIER)
			.withRequest(REQUEST_FILE)
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
				final var messageEntity = messageRepository.findById(messageId).orElseThrow();
				assertThat(messageEntity.getRecipients()).hasSize(3);
				assertThat(messageEntity.getRecipients()).allSatisfy(recipient -> {
					assertThat(recipient.getStatus()).isEqualTo(FAILED);
					assertThat(recipient.getMessageType()).isEqualTo(SMS);
				});
			});

	}

	@Test
	void test05_partially_successful_sendMultipleSms() {
		final var location = setupCall()
			.withServicePath("/%s/messages/sms".formatted(MUNICIPALITY_ID))
			.withHttpMethod(POST)
			.withHeader(Identifier.HEADER_NAME, IDENTIFIER)
			.withRequest(REQUEST_FILE)
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
				final var messageEntity = messageRepository.findById(messageId).orElseThrow();
				assertThat(messageEntity.getRecipients()).hasSize(3);
				assertThat(messageEntity.getRecipients())
					.filteredOn(recipient -> recipient.getStatus().equals(SENT))
					.hasSize(1);
				assertThat(messageEntity.getRecipients())
					.filteredOn(recipient -> recipient.getStatus().equals(FAILED))
					.hasSize(2);
			});

	}

	@Test
	void test06_employee_failure() {
		setupCall()
			.withServicePath("/%s/messages/sms".formatted(MUNICIPALITY_ID))
			.withHttpMethod(POST)
			.withHeader(Identifier.HEADER_NAME, IDENTIFIER)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_GATEWAY)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test07_messagingsettings_failure() {
		setupCall()
			.withServicePath("/%s/messages/sms".formatted(MUNICIPALITY_ID))
			.withHttpMethod(POST)
			.withHeader(Identifier.HEADER_NAME, IDENTIFIER)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_GATEWAY)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

}
