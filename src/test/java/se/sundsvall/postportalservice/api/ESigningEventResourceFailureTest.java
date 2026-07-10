package se.sundsvall.postportalservice.api;

import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.postportalservice.Application;
import se.sundsvall.postportalservice.api.model.SigningEvent;
import se.sundsvall.postportalservice.service.SigningEventService;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
@AutoConfigureWebTestClient
@ExtendWith(MockitoExtension.class)
class ESigningEventResourceFailureTest {

	@MockitoBean
	private SigningEventService signingEventServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	private static final String MESSAGE_ID = "550e8400-e29b-41d4-a716-446655440000";

	private static SigningEvent validEvent() {
		return SigningEvent.create().withProviderCaseId("1234567890").withEventType("CASE_COMPLETED").withStatus("SIGNED");
	}

	private static Stream<Arguments> badRequests() {
		return Stream.of(
			Arguments.of("2281", MESSAGE_ID, SigningEvent.create().withEventType("CASE_COMPLETED").withStatus("SIGNED")), // missing providerCaseId
			Arguments.of("2281", MESSAGE_ID, validEvent().withEventType("SOMETHING_ELSE")), // unknown event type
			Arguments.of("2281", MESSAGE_ID, validEvent().withStatus("BANANA")), // unknown status
			Arguments.of("2281", "not-a-uuid", validEvent()), // invalid message id
			Arguments.of("invalid", MESSAGE_ID, validEvent())); // invalid municipality id
	}

	@ParameterizedTest
	@MethodSource("badRequests")
	void receiveSigningEvent_badRequest(final String municipalityId, final String messageId, final SigningEvent event) {
		webTestClient.post()
			.uri("/" + municipalityId + "/e-signing/events/" + messageId)
			.bodyValue(event)
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON);

		verifyNoInteractions(signingEventServiceMock);
	}
}
