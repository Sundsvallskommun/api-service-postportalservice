package se.sundsvall.postportalservice.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.postportalservice.TestDataFactory.INVALID_MUNICIPALITY_ID;
import static se.sundsvall.postportalservice.TestDataFactory.MUNICIPALITY_ID;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zalando.problem.Problem;
import org.zalando.problem.violations.ConstraintViolationProblem;
import se.sundsvall.postportalservice.Application;
import se.sundsvall.postportalservice.api.model.MessageDetails;
import se.sundsvall.postportalservice.api.model.Messages;
import se.sundsvall.postportalservice.api.model.SigningInformation;
import se.sundsvall.postportalservice.service.HistoryService;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class HistoryResourceTest {

	@MockitoBean
	private HistoryService historyServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@AfterEach
	void teardown() {
		verifyNoMoreInteractions(historyServiceMock);
	}

	@Test
	void getMessageDetails_OK() {
		final var messageId = UUID.randomUUID().toString();
		final var userId = "joe01doe";

		final var messageDetails = new MessageDetails();

		when(historyServiceMock.getMessageDetails(MUNICIPALITY_ID, userId, messageId))
			.thenReturn(messageDetails);

		webTestClient.get()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/history/users/{userId}/messages/{messageId}")
				.build(MUNICIPALITY_ID, userId, messageId))
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.OK);

		verify(historyServiceMock).getMessageDetails(MUNICIPALITY_ID, userId, messageId);
	}

	@Test
	void getMessageDetails_NotFound() {
		final var messageId = UUID.randomUUID().toString();
		final var userId = "joe01doe";

		when(historyServiceMock.getMessageDetails(MUNICIPALITY_ID, userId, messageId)).thenThrow(Problem.valueOf(NOT_FOUND));

		webTestClient.get()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/history/users/{userId}/messages/{messageId}")
				.build(MUNICIPALITY_ID, userId, messageId))
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.NOT_FOUND);

		verify(historyServiceMock).getMessageDetails(MUNICIPALITY_ID, userId, messageId);
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
				assertThat(violation.getField()).isEqualTo("getMessageDetails.municipalityId");
				assertThat(violation.getMessage()).isEqualTo("not a valid municipality ID");
			},
			violation -> {
				assertThat(violation.getField()).isEqualTo("getMessageDetails.messageId");
				assertThat(violation.getMessage()).isEqualTo("not a valid UUID");
			});
	}

	@Test
	void getUserMessages_OK() {
		final var userId = "12345";
		final var messages = new Messages();
		final var pageableMock = Mockito.mock(Pageable.class);

		when(historyServiceMock.getUserMessages(eq(MUNICIPALITY_ID), eq(userId), any(Pageable.class))).thenReturn(messages);

		webTestClient.get()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/history/users/{userId}/messages")
				.build(MUNICIPALITY_ID, userId, pageableMock))
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.OK);

		verify(historyServiceMock).getUserMessages(eq(MUNICIPALITY_ID), eq(userId), any(Pageable.class));
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
				assertThat(violation.getField()).isEqualTo("getUserMessages.municipalityId");
				assertThat(violation.getMessage()).isEqualTo("not a valid municipality ID");
			});
	}

	@Test
	void getSigningInformation() {
		final var messageId = UUID.randomUUID().toString();

		final var status = "status";
		final var contentKey = "contentKey";
		final var orderReference = "orderReference";
		final var signature = "signature";
		final var oscpResponse = "ocspResponse";
		final var signedAt = OffsetDateTime.now();
		final var result = SigningInformation.create()
			.withStatus(status)
			.withContentKey(contentKey)
			.withOrderReference(orderReference)
			.withSignature(signature)
			.withOcspResponse(oscpResponse)
			.withSignedAt(signedAt);

		when(historyServiceMock.getSigningInformation(MUNICIPALITY_ID, messageId)).thenReturn(result);

		final var response = webTestClient.get()
			.uri("/{municipalityId}/history/messages/{messageId}/signinginfo", MUNICIPALITY_ID, messageId)
			.exchange()
			.expectBody(SigningInformation.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull().satisfies(signingInformation -> {
			assertThat(signingInformation.getStatus()).isEqualTo(status);
			assertThat(signingInformation.getContentKey()).isEqualTo(contentKey);
			assertThat(signingInformation.getOrderReference()).isEqualTo(orderReference);
			assertThat(signingInformation.getSignature()).isEqualTo(signature);
			assertThat(signingInformation.getOcspResponse()).isEqualTo(oscpResponse);
			assertThat(signingInformation.getSignedAt()).isEqualTo(signedAt);
		});

		verify(historyServiceMock).getSigningInformation(MUNICIPALITY_ID, messageId);
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
				assertThat(violation.getField()).isEqualTo("getSigningInformation.municipalityId");
				assertThat(violation.getMessage()).isEqualTo("not a valid municipality ID");
			},
			violation -> {
				assertThat(violation.getField()).isEqualTo("getSigningInformation.messageId");
				assertThat(violation.getMessage()).isEqualTo("not a valid UUID");
			});

	}

}
