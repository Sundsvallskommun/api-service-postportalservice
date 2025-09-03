package se.sundsvall.postportalservice.api;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static se.sundsvall.postportalservice.TestDataFactory.INVALID_MUNICIPALITY_ID;
import static se.sundsvall.postportalservice.TestDataFactory.MUNICIPALITY_ID;
import static se.sundsvall.postportalservice.TestDataFactory.createValidPrecheckRequest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.postportalservice.Application;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class PrecheckResourceTest {

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void precheckRecipients_OK() {
		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/precheck")
				.build(MUNICIPALITY_ID))
			.bodyValue(createValidPrecheckRequest())
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.NOT_IMPLEMENTED);
	}

	@Test
	void precheckRecipients_BadRequest() {
		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/precheck")
				.build(INVALID_MUNICIPALITY_ID))
			.bodyValue(createValidPrecheckRequest())
			.exchange()
			.expectStatus().isBadRequest();
	}

}
