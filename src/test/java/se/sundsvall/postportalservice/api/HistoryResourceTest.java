package se.sundsvall.postportalservice.api;

import java.time.OffsetDateTime;
import java.util.UUID;
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
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.postportalservice.Application;
import se.sundsvall.postportalservice.api.model.MessageDetails;
import se.sundsvall.postportalservice.api.model.Messages;
import se.sundsvall.postportalservice.api.model.SigningInformation;
import se.sundsvall.postportalservice.api.model.SigningStatus;
import se.sundsvall.postportalservice.service.HistoryService;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.ResponseEntity.ok;
import static se.sundsvall.postportalservice.TestDataFactory.MUNICIPALITY_ID;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
@AutoConfigureWebTestClient
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
	void getMessageDetails_WithSigningStatus() {
		final var messageId = UUID.randomUUID().toString();
		final var userId = "joe01doe";
		final var letterState = "SIGNED";
		final var signingProcessState = "COMPLETED";

		final var messageDetails = new MessageDetails()
			.withSubject("Test subject")
			.withSigningStatus(new SigningStatus()
				.withLetterState(letterState)
				.withSigningProcessState(signingProcessState));

		when(historyServiceMock.getMessageDetails(MUNICIPALITY_ID, userId, messageId))
			.thenReturn(messageDetails);

		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/history/users/{userId}/messages/{messageId}")
				.build(MUNICIPALITY_ID, userId, messageId))
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.OK)
			.expectBody(MessageDetails.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull().satisfies(details -> {
			assertThat(details.getSubject()).isEqualTo("Test subject");
			assertThat(details.getSigningStatus()).isNotNull().satisfies(status -> {
				assertThat(status.getLetterState()).isEqualTo(letterState);
				assertThat(status.getSigningProcessState()).isEqualTo(signingProcessState);
			});
		});

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
	void getSigningInformation() {
		final var messageId = UUID.randomUUID().toString();

		final var status = "status";
		final var contentKey = "contentKey";
		final var orderReference = "orderReference";
		final var signature = "signature";
		final var oscpResponse = "ocspResponse";
		final var signedAt = OffsetDateTime.of(2024, 6, 15, 12, 0, 0, 0, UTC);
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
	void readLetterReceipt() {
		final var messageId = UUID.randomUUID().toString();
		final var mockResponseEntity = ok()
			.header("Content-Type", "application/pdf")
			.body((StreamingResponseBody) outputStream -> outputStream.write("test data".getBytes()));

		when(historyServiceMock.getLetterReceipt(MUNICIPALITY_ID, messageId)).thenReturn(mockResponseEntity);

		final var bytes = webTestClient.get()
			.uri("/{municipalityId}/history/messages/{messageId}/receipt", MUNICIPALITY_ID, messageId)
			.exchange()
			.expectStatus().isOk()
			.expectBody(byte[].class)
			.returnResult()
			.getResponseBody();

		assertThat(bytes).isNotNull().isEqualTo("test data".getBytes());
		verify(historyServiceMock).getLetterReceipt(MUNICIPALITY_ID, messageId);
	}

	@Test
	void readLetterReceipt_notFound() {
		final var messageId = UUID.randomUUID().toString();

		when(historyServiceMock.getLetterReceipt(MUNICIPALITY_ID, messageId)).thenThrow(Problem.valueOf(NOT_FOUND));

		webTestClient.get()
			.uri("/{municipalityId}/history/messages/{messageId}/receipt", MUNICIPALITY_ID, messageId)
			.exchange()
			.expectStatus().isNotFound();

		verify(historyServiceMock).getLetterReceipt(MUNICIPALITY_ID, messageId);
	}

}
