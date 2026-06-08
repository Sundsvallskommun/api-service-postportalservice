package se.sundsvall.postportalservice.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.postportalservice.Application;
import se.sundsvall.postportalservice.service.AttachmentService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static se.sundsvall.postportalservice.TestDataFactory.INVALID_MUNICIPALITY_ID;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
@AutoConfigureWebTestClient
class AttachmentResourceFailureTest {

	@MockitoBean
	private AttachmentService attachmentServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@AfterEach
	void teardown() {
		verifyNoInteractions(attachmentServiceMock);
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
