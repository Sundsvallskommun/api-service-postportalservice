package se.sundsvall.postportalservice.api;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.postportalservice.Application;
import se.sundsvall.postportalservice.api.model.KivraEligibilityRequest;
import se.sundsvall.postportalservice.api.model.PrecheckRequest;
import se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration;
import se.sundsvall.postportalservice.service.PrecheckService;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.web.reactive.function.BodyInserters.fromMultipartData;
import static se.sundsvall.postportalservice.TestDataFactory.INVALID_MUNICIPALITY_ID;
import static se.sundsvall.postportalservice.TestDataFactory.MUNICIPALITY_ID;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
@AutoConfigureWebTestClient
class PrecheckResourceFailureTest {

	@MockitoBean
	private PrecheckService precheckServiceMock;

	@MockitoBean
	private MessagingSettingsIntegration messagingSettingsIntegrationMock;

	@Autowired
	private WebTestClient webTestClient;

	private static Stream<Arguments> precheckRecipientsBadBodyContentProvider() {

		return Stream.of(
			Arguments.of("Empty request", "{}", List.of(
				new Violation("partyIds", "must not be empty"))),

			Arguments.of("Empty party id list", new PrecheckRequest(emptyList()), List.of(
				new Violation("partyIds", "must not be empty"))),

			Arguments.of("Invalid party id in list", new PrecheckRequest(List.of("invalid")), List.of(
				new Violation("partyIds[0]", "not a valid UUID")))

		);
	}

	private static Stream<Arguments> checkKivraEligibilityBadBodyContentProvider() {
		return Stream.of(
			Arguments.of("Empty request", KivraEligibilityRequest.create(), List.of(
				new Violation("partyIds", "must not be empty"))),

			Arguments.of("Empty party id list", KivraEligibilityRequest.create().withPartyIds(emptyList()), List.of(
				new Violation("partyIds", "must not be empty"))),

			Arguments.of("Invalid party id in list", KivraEligibilityRequest.create().withPartyIds(List.of("invalid")), List.of(
				new Violation("partyIds[0]", "not a valid UUID"))));
	}

	@AfterEach
	void verifyNoUnexpectedMockInteractions() {
		verifyNoMoreInteractions(precheckServiceMock);
	}

	@Test
	void precheckCSV_BadPathContent() {
		final var multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("csv-file", "mockfile1").filename("legalIds.csv").contentType(MediaType.valueOf("text/csv"));

		final var response = webTestClient.post()
			.uri("/{municipalityId}/precheck/csv", INVALID_MUNICIPALITY_ID)
			.header(Identifier.HEADER_NAME, "type=adAccount; joe01doe")
			.contentType(MULTIPART_FORM_DATA)
			.body(fromMultipartData(multipartBodyBuilder.build()))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getViolations()).hasSize(1);

		verifyNoInteractions(precheckServiceMock);
	}

	@Test
	void precheckRecipients_BadHeaderContent() {
		final var request = new PrecheckRequest(List.of("b46f0ca2-d2ad-43e8-8d50-3aeb949e3604"));

		final var response = webTestClient.post()
			.uri("/{municipalityId}/precheck", MUNICIPALITY_ID)
			.contentType(APPLICATION_JSON)
			.accept(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response.getTitle()).isEqualTo("Bad Request");
		assertThat(response.getDetail()).isEqualTo("Required header 'X-Sent-By' is not present.");
	}

	@Test
	void precheckRecipients_BadPathContent() {
		final var request = new PrecheckRequest(List.of("b46f0ca2-d2ad-43e8-8d50-3aeb949e3604"));

		final var response = webTestClient.post()
			.uri("/{municipalityId}/precheck", INVALID_MUNICIPALITY_ID)
			.header(Identifier.HEADER_NAME, "type=adAccount; joe01doe")
			.contentType(APPLICATION_JSON)
			.accept(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getViolations()).hasSize(1).satisfiesExactly(violation -> {
			assertThat(violation.field()).isEqualTo("precheckRecipients.municipalityId");
			assertThat(violation.message()).isEqualTo("not a valid municipality ID");
		});

		verifyNoInteractions(precheckServiceMock);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("precheckRecipientsBadBodyContentProvider")
	void precheckRecipients_BadBodyContent(final String testDescription, final Object request, final List<Violation> expectedViolations) {
		final var response = webTestClient.post()
			.uri("/{municipalityId}/precheck", MUNICIPALITY_ID)
			.header(Identifier.HEADER_NAME, "type=adAccount; joe01doe")
			.contentType(APPLICATION_JSON)
			.accept(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getViolations()).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedViolations);

		verifyNoInteractions(precheckServiceMock);
	}

	@Test
	void checkKivraEligibility_BadPathContent() {
		final var request = new KivraEligibilityRequest()
			.withPartyIds(List.of("b46f0ca2-d2ad-43e8-8d50-3aeb949e3604"));

		final var response = webTestClient.post()
			.uri("/{municipalityId}/precheck/kivra", INVALID_MUNICIPALITY_ID)
			.header(Identifier.HEADER_NAME, "type=adAccount; joe01doe")
			.contentType(APPLICATION_JSON)
			.accept(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getViolations()).hasSize(1).satisfiesExactly(violation -> {
			assertThat(violation.field()).isEqualTo("checkKivraEligibility.municipalityId");
			assertThat(violation.message()).isEqualTo("not a valid municipality ID");
		});

		verifyNoInteractions(precheckServiceMock);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("checkKivraEligibilityBadBodyContentProvider")
	void checkKivraEligibility_BadBodyContent(final String testDescription, final Object request, final List<Violation> expectedViolations) {
		final var response = webTestClient.post()
			.uri("/{municipalityId}/precheck/kivra", MUNICIPALITY_ID)
			.header(Identifier.HEADER_NAME, "type=adAccount; joe01doe")
			.contentType(APPLICATION_JSON)
			.accept(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getViolations()).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedViolations);

		verifyNoInteractions(precheckServiceMock);
	}
}
