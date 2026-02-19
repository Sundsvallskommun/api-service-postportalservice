package se.sundsvall.postportalservice.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.postportalservice.Application;
import se.sundsvall.postportalservice.service.AttachmentService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static se.sundsvall.postportalservice.TestDataFactory.INVALID_MUNICIPALITY_ID;
import static se.sundsvall.postportalservice.TestDataFactory.MUNICIPALITY_ID;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
@AutoConfigureWebTestClient
class AttachmentResourceTest {

	@MockitoBean
	private AttachmentService attachmentServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@AfterEach
	void teardown() {
		verifyNoMoreInteractions(attachmentServiceMock);
	}

	@Test
	void getAttachmentsByMessageId_OK() {
		final var attachmentId = "3583f950-66fd-4b6d-85c8-95531654c9b4";
		final var contentDisposition = ContentDisposition.attachment()
			.filename("my-file.pdf")
			.build();
		final var mediaType = MediaType.APPLICATION_PDF;
		final var content = "hello".getBytes();
		final StreamingResponseBody body = os -> os.write(content);

		final var attachmentData = new AttachmentService.AttachmentData(contentDisposition, mediaType, body);

		when(attachmentServiceMock.getAttachmentData(MUNICIPALITY_ID, attachmentId)).thenReturn(attachmentData);

		webTestClient.get()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/attachments/{attachmentId}")
				.build(MUNICIPALITY_ID, attachmentId))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentDisposition(contentDisposition)
			.expectHeader().contentType(mediaType)
			.expectBody(byte[].class).isEqualTo(content);

		verify(attachmentServiceMock).getAttachmentData(MUNICIPALITY_ID, attachmentId);
	}

	@Test
	void getAttachmentsByMessageId_BadRequest() {
		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/attachments/{attachmentId}")
				.build(INVALID_MUNICIPALITY_ID, "invalid"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getViolations()).satisfiesExactlyInAnyOrder(violation -> {
			assertThat(violation.field()).isEqualTo("downloadAttachment.municipalityId");
			assertThat(violation.message()).isEqualTo("not a valid municipality ID");
		}, violation -> {
			assertThat(violation.field()).isEqualTo("downloadAttachment.attachmentId");
			assertThat(violation.message()).isEqualTo("not a valid UUID");
		});
	}
}
