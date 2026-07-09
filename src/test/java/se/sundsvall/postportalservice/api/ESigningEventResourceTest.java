package se.sundsvall.postportalservice.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.postportalservice.Application;
import se.sundsvall.postportalservice.api.model.EventSignatory;
import se.sundsvall.postportalservice.api.model.SignedDocument;
import se.sundsvall.postportalservice.api.model.SigningEvent;
import se.sundsvall.postportalservice.service.SigningEventService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static se.sundsvall.postportalservice.TestDataFactory.MUNICIPALITY_ID;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
@AutoConfigureWebTestClient
@ExtendWith(MockitoExtension.class)
class ESigningEventResourceTest {

	@MockitoBean
	private SigningEventService signingEventServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	private static final String MESSAGE_ID = "550e8400-e29b-41d4-a716-446655440000";

	@Test
	void receiveSigningEvent() {
		final var event = SigningEvent.create()
			.withCustomerReference(MESSAGE_ID)
			.withProviderCaseId("1234567890")
			.withProvider("comfact")
			.withEventType("CASE_COMPLETED")
			.withStatus("SIGNERAT")
			.withSignatory(EventSignatory.create().withPartyId("6d0773d6-3e7f-4552-81bc-f0007af95adf").withAction("APPROVED"))
			.withSignedDocument(SignedDocument.create().withFileName("signed.pdf").withContent("c2lnbmVk"));

		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path("/{municipalityId}/e-signing/events/{messageId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "messageId", MESSAGE_ID)))
			.bodyValue(event)
			.exchange()
			.expectStatus().isOk();

		verify(signingEventServiceMock).handleSigningEvent(eq(MUNICIPALITY_ID), eq(MESSAGE_ID), any(SigningEvent.class));
		verifyNoMoreInteractions(signingEventServiceMock);
	}
}
