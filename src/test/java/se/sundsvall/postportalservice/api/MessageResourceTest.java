package se.sundsvall.postportalservice.api;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PDF;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.web.reactive.function.BodyInserters.fromMultipartData;
import static se.sundsvall.postportalservice.TestDataFactory.INVALID_MUNICIPALITY_ID;
import static se.sundsvall.postportalservice.TestDataFactory.MUNICIPALITY_ID;
import static se.sundsvall.postportalservice.TestDataFactory.createValidDigitalRegisteredLetterRequest;
import static se.sundsvall.postportalservice.TestDataFactory.createValidLetterRequest;
import static se.sundsvall.postportalservice.TestDataFactory.createValidSmsRequest;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.multipart.MultipartFile;
import org.zalando.problem.Problem;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;
import se.sundsvall.postportalservice.Application;
import se.sundsvall.postportalservice.api.model.Address;
import se.sundsvall.postportalservice.api.model.DigitalRegisteredLetterRequest;
import se.sundsvall.postportalservice.api.model.LetterRequest;
import se.sundsvall.postportalservice.api.model.Recipient;
import se.sundsvall.postportalservice.api.model.Recipient.DeliveryMethod;
import se.sundsvall.postportalservice.api.model.SmsRecipient;
import se.sundsvall.postportalservice.api.model.SmsRequest;
import se.sundsvall.postportalservice.service.MessageService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class MessageResourceTest {

	@MockitoBean
	private MessageService messageServiceMock;

	@Captor
	private ArgumentCaptor<LetterRequest> letterRequestCaptor;

	@Captor
	private ArgumentCaptor<DigitalRegisteredLetterRequest> digitalRegisteredLRequestCaptor;

	@Captor
	private ArgumentCaptor<List<MultipartFile>> attachmentsArgumentCaptor;

	@Autowired
	private WebTestClient webTestClient;

	@AfterEach
	void verifyNoUnexpectedMockInteractions() {
		verifyNoMoreInteractions(messageServiceMock);
	}

	// ==================================================
	// Letter
	// ==================================================
	@Test
	void sendLetter_Created() {
		final var letterRequest = createValidLetterRequest();
		final var multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("request", letterRequest);
		multipartBodyBuilder.part("attachments", "mockFile").filename("test123.pdf").contentType(APPLICATION_PDF);

		when(messageServiceMock.processLetterRequest(eq(MUNICIPALITY_ID), letterRequestCaptor.capture(), attachmentsArgumentCaptor.capture())).thenReturn("messageId");

		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/messages/letter").build(MUNICIPALITY_ID))
			.header("X-Sent-By", "type=adAccount; joe01doe")
			.contentType(MULTIPART_FORM_DATA)
			.body(fromMultipartData(multipartBodyBuilder.build()))
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL_VALUE)
			.expectHeader().location("/2281/history/messages/messageId");

		final var capturedRequest = letterRequestCaptor.getValue();
		assertThat(capturedRequest).isEqualTo(letterRequest);
		final var capturedAttachments = attachmentsArgumentCaptor.getValue();
		assertThat(capturedAttachments).allSatisfy(file -> {
			assertThat(file.getOriginalFilename()).isEqualTo("test123.pdf");
			assertThat(file.getContentType()).isEqualTo(APPLICATION_PDF.toString());
		});

		verify(messageServiceMock).processLetterRequest(MUNICIPALITY_ID, capturedRequest, capturedAttachments);
	}

	@Test
	void sendLetter_BadHeaderContent() {
		final var letterRequest = createValidLetterRequest();
		final var multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("request", letterRequest);
		multipartBodyBuilder.part("attachments", "mockFile").filename("test123.pdf").contentType(APPLICATION_PDF);

		final var response = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/messages/letter").build(MUNICIPALITY_ID))
			.contentType(MULTIPART_FORM_DATA)
			.body(fromMultipartData(multipartBodyBuilder.build()))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response.getTitle()).isEqualTo("Bad Request");
		assertThat(response.getDetail()).isEqualTo("Required request header 'X-Sent-By' for method parameter type String is not present");
	}

	@Test
	void sendLetter_BadPathContent() {
		final var letterRequest = createValidLetterRequest();
		final var multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("request", letterRequest);
		multipartBodyBuilder.part("attachments", "mockFile").filename("test123.pdf").contentType(APPLICATION_PDF);

		final var response = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/messages/letter").build(INVALID_MUNICIPALITY_ID))
			.header("X-Sent-By", "type=adAccount; joe01doe")
			.contentType(MULTIPART_FORM_DATA)
			.body(fromMultipartData(multipartBodyBuilder.build()))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getViolations()).hasSize(1).satisfiesExactly(violation -> {
			assertThat(violation.getField()).isEqualTo("sendLetter.municipalityId");
			assertThat(violation.getMessage()).isEqualTo("not a valid municipality ID");
		});
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("sendLetterBadBodyContentProvider")
	void sendLetter_BadBodyContent(String testDescription, LetterRequest letterRequest, List<Violation> expectedViolations) {
		final var multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("request", letterRequest);
		multipartBodyBuilder.part("attachments", "mockFile").filename("test123.pdf").contentType(APPLICATION_PDF);

		final var response = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/messages/letter").build(MUNICIPALITY_ID))
			.header("X-Sent-By", "type=adAccount; joe01doe")
			.contentType(MULTIPART_FORM_DATA)
			.body(fromMultipartData(multipartBodyBuilder.build()))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getViolations()).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedViolations);
	}

	private static Stream<Arguments> sendLetterBadBodyContentProvider() {
		final var partyId = UUID.randomUUID().toString();

		return Stream.of(
			Arguments.of("Empty request", LetterRequest.create(), List.of(
				new Violation("body", "must not be blank"),
				new Violation("contentType", "must not be blank"),
				new Violation("subject", "must not be blank"))),

			Arguments.of("Empty address data", createRequestWithSetTopAttributes()
				.withAddresses(List.of(Address.create())),
				List.of(
					new Violation("addresses[0].city", "must not be blank"),
					new Violation("addresses[0].country", "must not be blank"),
					new Violation("addresses[0].firstName", "must not be blank"),
					new Violation("addresses[0].lastName", "must not be blank"),
					new Violation("addresses[0].street", "must not be blank"),
					new Violation("addresses[0].zipCode", "must not be blank"))),

			Arguments.of("Empty recipient data", createRequestWithSetTopAttributes()
				.withRecipients(List.of(Recipient.create())),
				List.of(
					new Violation("recipients[0].partyId", "not a valid UUID"),
					new Violation("recipients[0].deliveryMethod", "must not be null"))),

			Arguments.of("Invalid recipient party id", createRequestWithSetTopAttributes()
				.withRecipients(List.of(Recipient.create()
					.withPartyId("invalid"))),
				List.of(
					new Violation("recipients[0].partyId", "not a valid UUID"),
					new Violation("recipients[0].deliveryMethod", "must not be null"))),

			Arguments.of("Missing address for recipient with digital mail delivery", createRequestWithSetTopAttributes()
				.withRecipients(List.of(Recipient.create()
					.withDeliveryMethod(DeliveryMethod.DIGITAL_MAIL)
					.withPartyId("invalid"))),
				List.of(
					new Violation("recipients[0].partyId", "not a valid UUID"))),

			Arguments.of("Missing address for recipient with snail mail delivery", createRequestWithSetTopAttributes()
				.withRecipients(List.of(Recipient.create()
					.withDeliveryMethod(DeliveryMethod.SNAIL_MAIL)
					.withPartyId(partyId))),
				List.of(
					new Violation("recipients[0].address", "must not be null"))),

			Arguments.of("Empty address for recipient with snail mail delivery", createRequestWithSetTopAttributes()
				.withRecipients(List.of(Recipient.create()
					.withAddress(Address.create())
					.withDeliveryMethod(DeliveryMethod.SNAIL_MAIL)
					.withPartyId(partyId))),
				List.of(
					new Violation("recipients[0].address.city", "must not be blank"),
					new Violation("recipients[0].address.country", "must not be blank"),
					new Violation("recipients[0].address.firstName", "must not be blank"),
					new Violation("recipients[0].address.lastName", "must not be blank"),
					new Violation("recipients[0].address.street", "must not be blank"),
					new Violation("recipients[0].address.zipCode", "must not be blank"))));
	}

	private static LetterRequest createRequestWithSetTopAttributes() {
		return LetterRequest.create()
			.withBody("value")
			.withContentType("value")
			.withSubject("value");
	}

	@Test
	void sendLetter_NoAttachments() {
		final var multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("request", createValidLetterRequest());

		final var response = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/messages/letter").build(MUNICIPALITY_ID))
			.header("X-Sent-By", "type=adAccount; joe01doe")
			.contentType(MULTIPART_FORM_DATA)
			.body(fromMultipartData(multipartBodyBuilder.build()))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response.getTitle()).isEqualTo("Bad Request");
		assertThat(response.getDetail()).isEqualTo("Required part 'attachments' is not present.");
	}

	@Test
	void sendLetter_DuplicateFilenames() {
		final var multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("request", createValidLetterRequest());
		multipartBodyBuilder.part("attachments", "mockfile1").filename("file1.pdf").contentType(APPLICATION_PDF);
		multipartBodyBuilder.part("attachments", "mockfile1").filename("file1.pdf").contentType(APPLICATION_PDF);

		final var response = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/messages/letter").build(MUNICIPALITY_ID))
			.header("X-Sent-By", "type=adAccount; joe01doe")
			.contentType(MULTIPART_FORM_DATA)
			.body(fromMultipartData(multipartBodyBuilder.build()))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getViolations()).satisfiesExactly(violation -> {
			assertThat(violation.getField()).isEqualTo("sendLetter.attachments");
			assertThat(violation.getMessage()).isEqualTo("no duplicate file names allowed in the list of files");
		});
	}

	// ==================================================
	// Digital registered letter
	// ==================================================
	@Test
	void sendDigitalRegisteredLetter_Created() {
		final var digitalRegisteredLetterRequest = createValidDigitalRegisteredLetterRequest();
		final var multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("request", digitalRegisteredLetterRequest, APPLICATION_JSON);
		multipartBodyBuilder.part("attachments", "mockFile").filename("test123.pdf").contentType(APPLICATION_PDF);

		when(messageServiceMock.processDigitalRegisteredLetterRequest(eq(MUNICIPALITY_ID), digitalRegisteredLRequestCaptor.capture(), attachmentsArgumentCaptor.capture())).thenReturn("messageId");

		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/messages/registered-letter")
				.build(MUNICIPALITY_ID))
			.contentType(MULTIPART_FORM_DATA)
			.header("X-Sent-By", "type=adAccount; joe01doe")
			.body(fromMultipartData(multipartBodyBuilder.build()))
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL_VALUE)
			.expectHeader().location("/2281/history/messages/messageId");

		final var capturedRequest = digitalRegisteredLRequestCaptor.getValue();
		assertThat(capturedRequest).isEqualTo(digitalRegisteredLetterRequest);
		final var capturedAttachments = attachmentsArgumentCaptor.getValue();
		assertThat(capturedAttachments).allSatisfy(file -> {
			assertThat(file.getOriginalFilename()).isEqualTo("test123.pdf");
			assertThat(file.getContentType()).isEqualTo(APPLICATION_PDF.toString());
		});

		verify(messageServiceMock).processDigitalRegisteredLetterRequest(MUNICIPALITY_ID, capturedRequest, capturedAttachments);
	}

	@Test
	void sendDigitalRegisteredLetter_BadHeaderContent() {
		final var multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("request", createValidDigitalRegisteredLetterRequest(), APPLICATION_JSON);
		multipartBodyBuilder.part("attachments", "mockFile").filename("test123.pdf").contentType(APPLICATION_PDF);

		final var response = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/messages/registered-letter").build(MUNICIPALITY_ID))
			.contentType(MULTIPART_FORM_DATA)
			.body(fromMultipartData(multipartBodyBuilder.build()))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response.getTitle()).isEqualTo("Bad Request");
		assertThat(response.getDetail()).isEqualTo("Required request header 'X-Sent-By' for method parameter type String is not present");
	}

	@Test
	void sendDigitalRegisteredLetter_BadPathContent() {
		final var multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("request", createValidDigitalRegisteredLetterRequest());
		multipartBodyBuilder.part("attachments", "mockFile").filename("test123.pdf").contentType(APPLICATION_PDF);

		final var response = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/messages/registered-letter").build(INVALID_MUNICIPALITY_ID))
			.header("X-Sent-By", "type=adAccount; joe01doe")
			.contentType(MULTIPART_FORM_DATA)
			.body(fromMultipartData(multipartBodyBuilder.build()))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getViolations()).hasSize(1).satisfiesExactly(violation -> {
			assertThat(violation.getField()).isEqualTo("sendDigitalRegisteredLetter.municipalityId");
			assertThat(violation.getMessage()).isEqualTo("not a valid municipality ID");
		});
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("sendDigitalRegisteredLetterBadBodyContentProvider")
	void sendDigitalRegisteredLetter_BadBodyContent(String testDescription, DigitalRegisteredLetterRequest digitalRegisteredLetterRequest, List<Violation> expectedViolations) {
		final var multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("request", digitalRegisteredLetterRequest);
		multipartBodyBuilder.part("attachments", "mockFile").filename("test123.pdf").contentType(APPLICATION_PDF);

		final var response = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/messages/registered-letter").build(MUNICIPALITY_ID))
			.header("X-Sent-By", "type=adAccount; joe01doe")
			.contentType(MULTIPART_FORM_DATA)
			.body(fromMultipartData(multipartBodyBuilder.build()))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getViolations()).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedViolations);
	}

	private static Stream<Arguments> sendDigitalRegisteredLetterBadBodyContentProvider() {
		return Stream.of(
			Arguments.of("Empty request",
				DigitalRegisteredLetterRequest.create(), List.of(
					new Violation("body", "must not be blank"),
					new Violation("contentType", "must be one of: [text/plain, text/html]"),
					new Violation("partyId", "not a valid UUID"),
					new Violation("subject", "must not be blank")),

				Arguments.of("Invalid content type and party id and space for the other attributes", DigitalRegisteredLetterRequest.create()
					.withBody(" ")
					.withContentType("invalid")
					.withPartyId("invalid")
					.withSubject(" "),
					List.of(
						new Violation("body", "must not be blank"),
						new Violation("contentType", "must be one of: [text/plain, text/html]"),
						new Violation("partyId", "not a valid UUID"),
						new Violation("subject", "must not be blank")))));
	}

	@Test
	void sendDigitalRegisteredLetter_NoAttachments() {
		final var multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("request", createValidDigitalRegisteredLetterRequest());

		final var response = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/messages/registered-letter").build(MUNICIPALITY_ID))
			.header("X-Sent-By", "type=adAccount; joe01doe")
			.contentType(MULTIPART_FORM_DATA)
			.body(fromMultipartData(multipartBodyBuilder.build()))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response.getTitle()).isEqualTo("Bad Request");
		assertThat(response.getDetail()).isEqualTo("Required part 'attachments' is not present.");
	}

	@Test
	void sendDigitalRegisteredLetter_DuplicateFilenames() {
		final var multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("request", createValidDigitalRegisteredLetterRequest());
		multipartBodyBuilder.part("attachments", "mockfile1").filename("file1.pdf").contentType(APPLICATION_PDF);
		multipartBodyBuilder.part("attachments", "mockfile1").filename("file1.pdf").contentType(APPLICATION_PDF);

		final var response = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/messages/registered-letter").build(MUNICIPALITY_ID))
			.header("X-Sent-By", "type=adAccount; joe01doe")
			.contentType(MULTIPART_FORM_DATA)
			.body(fromMultipartData(multipartBodyBuilder.build()))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getViolations()).satisfiesExactly(violation -> {
			assertThat(violation.getField()).isEqualTo("sendDigitalRegisteredLetter.attachments");
			assertThat(violation.getMessage()).isEqualTo("no duplicate file names allowed in the list of files");
		});
	}

	@Test
	void sendDigitalRegisteredLetter_NotPdfFile() {
		final var multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("request", createValidDigitalRegisteredLetterRequest());
		multipartBodyBuilder.part("attachments", "mockfile").filename("file1.xml").contentType(APPLICATION_XML);

		final var response = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/messages/registered-letter").build(MUNICIPALITY_ID))
			.header("X-Sent-By", "type=adAccount; joe01doe")
			.contentType(MULTIPART_FORM_DATA)
			.body(fromMultipartData(multipartBodyBuilder.build()))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getViolations()).satisfiesExactly(violation -> {
			assertThat(violation.getField()).isEqualTo("sendDigitalRegisteredLetter.attachments");
			assertThat(violation.getMessage()).isEqualTo("content type must be application/pdf");
		});
	}

	// ==================================================
	// Sms
	// ==================================================
	@Test
	void sendSms_Created() {
		final var request = createValidSmsRequest();
		when(messageServiceMock.processSmsRequest(MUNICIPALITY_ID, request)).thenReturn("messageId");

		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/messages/sms")
				.build(MUNICIPALITY_ID))
			.header("X-Sent-By", "type=adAccount; joe01doe")
			.bodyValue(request)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL_VALUE)
			.expectHeader().location("/2281/history/messages/messageId");

		verify(messageServiceMock).processSmsRequest(MUNICIPALITY_ID, request);
	}

	@Test
	void sendSms_BadHeaderContent() {
		final var response = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/messages/sms").build(MUNICIPALITY_ID))
			.bodyValue(createValidSmsRequest())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response.getTitle()).isEqualTo("Bad Request");
		assertThat(response.getDetail()).isEqualTo("Required request header 'X-Sent-By' for method parameter type String is not present");
	}

	@Test
	void sendSms_BadPathContent() {
		final var response = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/messages/sms").build(INVALID_MUNICIPALITY_ID))
			.header("X-Sent-By", "type=adAccount; joe01doe")
			.bodyValue(createValidSmsRequest())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getViolations()).hasSize(1).satisfiesExactly(violation -> {
			assertThat(violation.getField()).isEqualTo("sendSms.municipalityId");
			assertThat(violation.getMessage()).isEqualTo("not a valid municipality ID");
		});
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("sendSmsBadBodyContentProvider")
	void sendSms_BadBodyContent(String testDescription, SmsRequest smsRequest, List<Violation> expectedViolations) {

		final var response = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/messages/sms").build(MUNICIPALITY_ID))
			.header("X-Sent-By", "type=adAccount; joe01doe")
			.bodyValue(smsRequest)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getViolations()).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedViolations);
	}

	private static Stream<Arguments> sendSmsBadBodyContentProvider() {
		return Stream.of(
			Arguments.of("Empty request", SmsRequest.create(), List.of(
				new Violation("message", "must not be blank"),
				new Violation("recipients", "must not be empty"))),

			Arguments.of("Request with blank message and empty recipient list",
				SmsRequest.create()
					.withMessage(" ")
					.withRecipients(emptyList()),
				List.of(
					new Violation("message", "must not be blank"),
					new Violation("recipients", "must not be empty"))),

			Arguments.of("Request empty recipient",
				SmsRequest.create()
					.withMessage("message")
					.withRecipients(List.of(SmsRecipient.create())),
				List.of(
					new Violation("recipients[0].phoneNumber", "must be a valid MSISDN (example: +46701740605). Regular expression: ^\\+[1-9][\\d]{3,14}$"))),

			Arguments.of("Request where recipient has invalid attribute values",
				SmsRequest.create()
					.withMessage("message")
					.withRecipients(List.of(SmsRecipient.create()
						.withPartyId("invalid")
						.withPhoneNumber("invalid"))),
				List.of(
					new Violation("recipients[0].phoneNumber", "must be a valid MSISDN (example: +46701740605). Regular expression: ^\\+[1-9][\\d]{3,14}$"))));
	}

}
