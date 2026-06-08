package se.sundsvall.postportalservice.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.postportalservice.Application;
import se.sundsvall.postportalservice.service.HistoryService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static se.sundsvall.postportalservice.TestDataFactory.INVALID_MUNICIPALITY_ID;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
@AutoConfigureWebTestClient
class HistoryResourceFailureTest {

	@MockitoBean
	private HistoryService historyServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@AfterEach
	void teardown() {
		verifyNoInteractions(historyServiceMock);
	}

	@Test
	void getMessageDetails_BadRequest() {
		final var messageId = "invalid";
		final var userId = "joe01doe";

		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/history/users/{userId}/messages/{messageId}")
				.build(INVALID_MUNICIPALITY_ID, userId, messageId))
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getViolations()).hasSize(2).satisfiesExactlyInAnyOrder(
			violation -> {
				assertThat(violation.field()).isEqualTo("getMessageDetails.municipalityId");
				assertThat(violation.message()).isEqualTo("not a valid municipality ID");
			},
			violation -> {
				assertThat(violation.field()).isEqualTo("getMessageDetails.messageId");
				assertThat(violation.message()).isEqualTo("not a valid UUID");
			});
	}

	@Test
	void getUserMessages_BadRequest() {
		final var userId = "12345";
		final var pageableMock = Mockito.mock(Pageable.class);

		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/history/users/{userId}/messages")
				.build(INVALID_MUNICIPALITY_ID, userId, pageableMock))
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getViolations()).hasSize(1).satisfiesExactlyInAnyOrder(
			violation -> {
				assertThat(violation.field()).isEqualTo("getUserMessages.municipalityId");
				assertThat(violation.message()).isEqualTo("not a valid municipality ID");
			});
	}

	@Test
	void getSigningInformation_badRequest() {
		final var response = webTestClient.get()
			.uri("/{municipalityId}/history/messages/{messageId}/signinginfo", INVALID_MUNICIPALITY_ID, "invalid")
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getViolations()).hasSize(2).satisfiesExactlyInAnyOrder(
			violation -> {
				assertThat(violation.field()).isEqualTo("getSigningInformation.municipalityId");
				assertThat(violation.message()).isEqualTo("not a valid municipality ID");
			},
			violation -> {
				assertThat(violation.field()).isEqualTo("getSigningInformation.messageId");
				assertThat(violation.message()).isEqualTo("not a valid UUID");
			});

	}

	@Test
	void readLetterReceipt_badRequest() {
		final var response = webTestClient.get()
			.uri("/{municipalityId}/history/messages/{messageId}/receipt", INVALID_MUNICIPALITY_ID, "invalid")
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getViolations()).hasSize(2).satisfiesExactlyInAnyOrder(
			violation -> {
				assertThat(violation.field()).isEqualTo("readLetterReceipt.municipalityId");
				assertThat(violation.message()).isEqualTo("not a valid municipality ID");
			},
			violation -> {
				assertThat(violation.field()).isEqualTo("readLetterReceipt.messageId");
				assertThat(violation.message()).isEqualTo("not a valid UUID");
			});
	}

}
