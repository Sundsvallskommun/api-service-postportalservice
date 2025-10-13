package se.sundsvall.postportalservice.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PDF;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.web.reactive.function.BodyInserters.fromMultipartData;
import static se.sundsvall.postportalservice.TestDataFactory.INVALID_MUNICIPALITY_ID;
import static se.sundsvall.postportalservice.TestDataFactory.MUNICIPALITY_ID;
import static se.sundsvall.postportalservice.TestDataFactory.createValidDigitalRegisteredLetterRequest;
import static se.sundsvall.postportalservice.TestDataFactory.createValidLetterCsvRequest;
import static se.sundsvall.postportalservice.TestDataFactory.createValidLetterRequest;
import static se.sundsvall.postportalservice.TestDataFactory.createValidSmsRequest;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.postportalservice.Application;
import se.sundsvall.postportalservice.api.model.LetterCsvRequest;
import se.sundsvall.postportalservice.api.model.LetterRequest;
import se.sundsvall.postportalservice.service.MessageService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class MessageResourceTest {

	@MockitoBean
	private MessageService messageServiceMock;

	@Captor
	private ArgumentCaptor<LetterRequest> letterRequestCaptor;

	@Captor
	private ArgumentCaptor<LetterCsvRequest> letterCsvRequestCaptor;

	@Captor
	private ArgumentCaptor<List<MultipartFile>> multiPartFilesCaptor;

	@Captor
	private ArgumentCaptor<MultipartFile> multipartFileCaptor;

	@Autowired
	private WebTestClient webTestClient;

	@AfterEach
	void verifyNoUnexpectedMockInteractions() {
		verifyNoMoreInteractions(messageServiceMock);
	}

	@Test
	void sendLetter_Created() {
		final var userId = "12345";
		final var messageId = "12345";

		final var multipartBodyBuilder = new MultipartBodyBuilder();
		final var letterRequest = createValidLetterRequest();

		multipartBodyBuilder.part("request", letterRequest);
		multipartBodyBuilder.part("attachments", "mockFile").filename("test123.pdf").contentType(APPLICATION_PDF);

		when(messageServiceMock.processLetterRequest(eq(MUNICIPALITY_ID), letterRequestCaptor.capture(), multiPartFilesCaptor.capture())).thenReturn("messageId");

		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/messages/letter")
				.build(MUNICIPALITY_ID, userId, messageId))
			.header("X-Sent-By", "type=adAccount; joe01doe")
			.contentType(MULTIPART_FORM_DATA)
			.body(fromMultipartData(multipartBodyBuilder.build()))
			.exchange()
			.expectStatus().isEqualTo(CREATED);

		var capturedRequest = letterRequestCaptor.getValue();
		assertThat(capturedRequest).isEqualTo(letterRequest);
		var capturedAttachments = multiPartFilesCaptor.getValue();
		assertThat(capturedAttachments).allSatisfy(file -> {
			assertThat(file.getOriginalFilename()).isEqualTo("test123.pdf");
			assertThat(file.getContentType()).isEqualTo(APPLICATION_PDF.toString());
		});

		verify(messageServiceMock).processLetterRequest(MUNICIPALITY_ID, capturedRequest, capturedAttachments);

	}

	@Test
	void sendLetter_BadRequest() {
		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/messages/letter")
				.build(INVALID_MUNICIPALITY_ID))
			.bodyValue(createValidLetterRequest())
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void sendLetterCsv_Created() {
		final var userId = "12345";
		final var messageId = "12345";

		final var multipartBodyBuilder = new MultipartBodyBuilder();
		final var request = createValidLetterCsvRequest();

		multipartBodyBuilder.part("request", request);
		multipartBodyBuilder.part("csv-file", "mockFile").filename("test123.csv").contentType(MediaType.valueOf("text/csv"));
		multipartBodyBuilder.part("attachments", "mockFile").filename("test123.pdf").contentType(APPLICATION_PDF);

		when(messageServiceMock.processCsvLetterRequest(eq(MUNICIPALITY_ID), letterCsvRequestCaptor.capture(), multipartFileCaptor.capture(), multiPartFilesCaptor.capture()))
			.thenReturn("messageId");

		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/messages/letter/csv")
				.build(MUNICIPALITY_ID, userId, messageId))
			.header("X-Sent-By", "type=adAccount; joe01doe")
			.contentType(MULTIPART_FORM_DATA)
			.body(fromMultipartData(multipartBodyBuilder.build()))
			.exchange()
			.expectStatus().isEqualTo(CREATED);

		var capturedRequest = letterCsvRequestCaptor.getValue();
		assertThat(capturedRequest).isEqualTo(request);
		var capturedCsvFile = multipartFileCaptor.getValue();
		assertThat(capturedCsvFile.getOriginalFilename()).isEqualTo("test123.csv");
		assertThat(capturedCsvFile.getContentType()).isEqualTo("text/csv");
		var capturedAttachments = multiPartFilesCaptor.getValue();
		assertThat(capturedAttachments).allSatisfy(file -> {
			assertThat(file.getOriginalFilename()).isEqualTo("test123.pdf");
			assertThat(file.getContentType()).isEqualTo(APPLICATION_PDF.toString());
		});

		verify(messageServiceMock).processCsvLetterRequest(MUNICIPALITY_ID, capturedRequest, capturedCsvFile, capturedAttachments);
	}

	@Test
	void sendLetterCsv_BadRequest() {
		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/messages/letter/csv")
				.build(INVALID_MUNICIPALITY_ID))
			.bodyValue(createValidLetterRequest())
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void sendDigitalRegisteredLetter_OK() {
		final var multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("request", createValidDigitalRegisteredLetterRequest(), APPLICATION_JSON);
		multipartBodyBuilder.part("attachments", "mockFile").filename("test123.pdf").contentType(APPLICATION_PDF);

		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/messages/registered-letter")
				.build(MUNICIPALITY_ID))
			.contentType(MULTIPART_FORM_DATA)
			.header("X-Sent-By", "type=adAccount; joe01doe")
			.body(fromMultipartData(multipartBodyBuilder.build()))
			.exchange()
			.expectStatus().isEqualTo(CREATED);

		verify(messageServiceMock).processDigitalRegisteredLetterRequest(any(), any(), any());
	}

	@Test
	void sendDigitalRegisteredLetter_BadRequest() {
		final var multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("request", createValidDigitalRegisteredLetterRequest(), APPLICATION_JSON);
		multipartBodyBuilder.part("attachments", "mockFile").filename("test123.pdf").contentType(APPLICATION_PDF);

		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/messages/registered-letter")
				.build(INVALID_MUNICIPALITY_ID))
			.contentType(MULTIPART_FORM_DATA)
			.body(fromMultipartData(multipartBodyBuilder.build()))
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void sendSms_Created() {
		var request = createValidSmsRequest();
		when(messageServiceMock.processSmsRequest(MUNICIPALITY_ID, request)).thenReturn("messageId");

		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/messages/sms")
				.build(MUNICIPALITY_ID))
			.header("X-Sent-By", "type=adAccount; joe01doe")
			.bodyValue(request)
			.exchange()
			.expectStatus().isEqualTo(CREATED);

		verify(messageServiceMock).processSmsRequest(MUNICIPALITY_ID, request);
	}

	@Test
	void sendSms_BadRequest() {
		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/messages/sms")
				.build(INVALID_MUNICIPALITY_ID))
			.bodyValue(createValidSmsRequest())
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
	}
}
