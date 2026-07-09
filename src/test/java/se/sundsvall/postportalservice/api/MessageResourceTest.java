package se.sundsvall.postportalservice.api;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.postportalservice.Application;
import se.sundsvall.postportalservice.api.model.DigitalRegisteredLetterRequest;
import se.sundsvall.postportalservice.api.model.ESigningRequest;
import se.sundsvall.postportalservice.api.model.ESigningSignatory;
import se.sundsvall.postportalservice.api.model.LetterRequest;
import se.sundsvall.postportalservice.api.model.SmsCsvRequest;
import se.sundsvall.postportalservice.service.MessageService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PDF;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.http.MediaType.parseMediaType;
import static org.springframework.web.reactive.function.BodyInserters.fromMultipartData;
import static se.sundsvall.postportalservice.TestDataFactory.MUNICIPALITY_ID;
import static se.sundsvall.postportalservice.TestDataFactory.createValidDigitalRegisteredLetterRequest;
import static se.sundsvall.postportalservice.TestDataFactory.createValidLetterRequest;
import static se.sundsvall.postportalservice.TestDataFactory.createValidSmsCsvRequest;
import static se.sundsvall.postportalservice.TestDataFactory.createValidSmsRequest;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
@AutoConfigureWebTestClient
@ExtendWith(MockitoExtension.class)
class MessageResourceTest {

	@MockitoBean
	private MessageService messageServiceMock;

	@Captor
	private ArgumentCaptor<LetterRequest> letterRequestCaptor;

	@Captor
	private ArgumentCaptor<DigitalRegisteredLetterRequest> digitalRegisteredLRequestCaptor;

	@Captor
	private ArgumentCaptor<ESigningRequest> esigningRequestCaptor;

	@Captor
	private ArgumentCaptor<MultipartFile> documentArgumentCaptor;

	@Captor
	private ArgumentCaptor<List<MultipartFile>> attachmentsArgumentCaptor;

	@Autowired
	private WebTestClient webTestClient;

	@AfterEach
	void verifyNoUnexpectedMockInteractions() {
		verifyNoMoreInteractions(messageServiceMock);
	}

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
			.expectHeader().location("/2281/history/users/joe01doe/messages/messageId");

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
			.expectHeader().location("/2281/history/users/joe01doe/messages/messageId");

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
	void sendESigning_Created() {
		final var esigningRequest = ESigningRequest.create()
			.withSubject("Please sign")
			.withBody("Please sign the document")
			.withSignatories(List.of(ESigningSignatory.create()
				.withPartyId("6d0773d6-3e7f-4552-81bc-f0007af95adf").withName("John Doe").withEmail("john@sundsvall.se")));
		final var multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("request", esigningRequest, APPLICATION_JSON);
		multipartBodyBuilder.part("document", "documentFile").filename("document.pdf").contentType(APPLICATION_PDF);
		multipartBodyBuilder.part("attachments", "attachmentFile").filename("attachment.pdf").contentType(APPLICATION_PDF);

		when(messageServiceMock.processESigningRequest(eq(MUNICIPALITY_ID), esigningRequestCaptor.capture(), documentArgumentCaptor.capture(), attachmentsArgumentCaptor.capture())).thenReturn("messageId");

		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/messages/e-signing")
				.build(MUNICIPALITY_ID))
			.contentType(MULTIPART_FORM_DATA)
			.header("X-Sent-By", "type=adAccount; joe01doe")
			.body(fromMultipartData(multipartBodyBuilder.build()))
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL_VALUE)
			.expectHeader().location("/2281/history/users/joe01doe/messages/messageId");

		final var capturedRequest = esigningRequestCaptor.getValue();
		assertThat(capturedRequest).isEqualTo(esigningRequest);
		final var capturedDocument = documentArgumentCaptor.getValue();
		assertThat(capturedDocument.getOriginalFilename()).isEqualTo("document.pdf");
		assertThat(capturedDocument.getContentType()).isEqualTo(APPLICATION_PDF.toString());
		final var capturedAttachments = attachmentsArgumentCaptor.getValue();
		assertThat(capturedAttachments).allSatisfy(file -> {
			assertThat(file.getOriginalFilename()).isEqualTo("attachment.pdf");
			assertThat(file.getContentType()).isEqualTo(APPLICATION_PDF.toString());
		});

		verify(messageServiceMock).processESigningRequest(MUNICIPALITY_ID, capturedRequest, capturedDocument, capturedAttachments);
	}

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
			.expectHeader().location("/2281/history/users/joe01doe/messages/messageId");

		verify(messageServiceMock).processSmsRequest(MUNICIPALITY_ID, request);
	}

	@Test
	void sendSmsCsv_Created() {
		final var request = createValidSmsCsvRequest();
		final var multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("request", request);
		multipartBodyBuilder.part("csv-file", "Phonenumber\n+46701740605\n").filename("phones.csv").contentType(parseMediaType("text/csv"));

		when(messageServiceMock.processCsvSmsRequest(eq(MUNICIPALITY_ID), any(SmsCsvRequest.class), any(MultipartFile.class))).thenReturn("messageId");

		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/messages/sms/csv").build(MUNICIPALITY_ID))
			.header("X-Sent-By", "type=adAccount; joe01doe")
			.contentType(MULTIPART_FORM_DATA)
			.body(fromMultipartData(multipartBodyBuilder.build()))
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL_VALUE)
			.expectHeader().location("/2281/history/users/joe01doe/messages/messageId");

		verify(messageServiceMock).processCsvSmsRequest(eq(MUNICIPALITY_ID), any(SmsCsvRequest.class), any(MultipartFile.class));
	}

	@Test
	void cancelESigning_NoContent() {
		final var messageId = "9ce333ec-a473-438b-8406-a71e957dc107";

		webTestClient.delete()
			.uri("/{municipalityId}/messages/e-signing/{messageId}", MUNICIPALITY_ID, messageId)
			.exchange()
			.expectStatus().isNoContent();

		verify(messageServiceMock).cancelESigning(MUNICIPALITY_ID, messageId);
	}

}
