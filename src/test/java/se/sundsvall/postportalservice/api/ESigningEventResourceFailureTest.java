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

	private static SigningEvent validEvent() {
		return SigningEvent.create().withProviderCaseId("1234567890").withEventType("CASE_COMPLETED").withStatus("SIGNERAT");
	}

	private static Stream<Arguments> badRequests() {
		return Stream.of(
			Arguments.of("2281", SigningEvent.create().withEventType("CASE_COMPLETED").withStatus("SIGNERAT")), // missing providerCaseId
			Arguments.of("2281", validEvent().withEventType("SOMETHING_ELSE")), // unknown event type
			Arguments.of("2281", validEvent().withStatus("BANANA")), // unknown status
			Arguments.of("invalid", validEvent())); // invalid municipality id
	}

	@ParameterizedTest
	@MethodSource("badRequests")
	void receiveSigningEvent_badRequest(final String municipalityId, final SigningEvent event) {
		webTestClient.post()
			.uri("/" + municipalityId + "/e-signing/events")
			.bodyValue(event)
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON);

		verifyNoInteractions(signingEventServiceMock);
	}
}
