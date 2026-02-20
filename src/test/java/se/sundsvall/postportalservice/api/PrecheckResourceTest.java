package se.sundsvall.postportalservice.api;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.postportalservice.Application;
import se.sundsvall.postportalservice.api.model.KivraEligibilityRequest;
import se.sundsvall.postportalservice.api.model.PrecheckCsvResponse;
import se.sundsvall.postportalservice.api.model.PrecheckRequest;
import se.sundsvall.postportalservice.api.model.PrecheckResponse;
import se.sundsvall.postportalservice.api.model.PrecheckResponse.DeliveryMethod;
import se.sundsvall.postportalservice.api.model.PrecheckResponse.PrecheckRecipient;
import se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration;
import se.sundsvall.postportalservice.service.PrecheckService;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.web.reactive.function.BodyInserters.fromMultipartData;
import static se.sundsvall.postportalservice.TestDataFactory.INVALID_MUNICIPALITY_ID;
import static se.sundsvall.postportalservice.TestDataFactory.MUNICIPALITY_ID;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
@AutoConfigureWebTestClient
class PrecheckResourceTest {

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
	void precheckCSV() {
		final var multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("csv-file", "mockfile1").filename("legalIds.csv").contentType(MediaType.valueOf("text/csv"));

		when(precheckServiceMock.precheckCSV(eq(MUNICIPALITY_ID), any(MultipartFile.class))).thenReturn(new PrecheckCsvResponse(Map.of("201901012391", 2, "201901022382", 3), Set.of()));

		final var response = webTestClient.post()
			.uri("/{municipalityId}/precheck/csv", MUNICIPALITY_ID)
			.header(Identifier.HEADER_NAME, "type=adAccount; joe01doe")
			.contentType(MULTIPART_FORM_DATA)
			.body(fromMultipartData(multipartBodyBuilder.build()))
			.exchange()
			.expectStatus().isOk()
			.expectBody(PrecheckCsvResponse.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.duplicateEntries()).containsExactlyInAnyOrderEntriesOf(Map.of("201901012391", 2, "201901022382", 3));
		verify(precheckServiceMock).precheckCSV(eq(MUNICIPALITY_ID), any(MultipartFile.class));
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
	void precheckPartyIds() {
		final var request = new PrecheckRequest(List.of("b46f0ca2-d2ad-43e8-8d50-3aeb949e3604", "fd99a03c-790c-4b87-bc4b-f4f73e4a2df4"));

		final var precheckResponse = PrecheckResponse.of(List.of(
			new PrecheckRecipient("b46f0ca2-d2ad-43e8-8d50-3aeb949e3604", "partyId-1", DeliveryMethod.DIGITAL_MAIL, null),
			new PrecheckRecipient("fd99a03c-790c-4b87-bc4b-f4f73e4a2df4", "partyId-2", DeliveryMethod.DIGITAL_MAIL, null)));

		when(precheckServiceMock.precheckPartyIds(MUNICIPALITY_ID, request.partyIds())).thenReturn(precheckResponse);

		final var response = webTestClient.post()
			.uri("/{municipalityId}/precheck", MUNICIPALITY_ID)
			.header(Identifier.HEADER_NAME, "type=adAccount; joe01doe")
			.contentType(APPLICATION_JSON)
			.accept(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isOk()
			.expectBody(PrecheckResponse.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.precheckRecipients()).extracting("personalIdentityNumber", "partyId", "deliveryMethod").containsExactlyInAnyOrder(
			tuple("b46f0ca2-d2ad-43e8-8d50-3aeb949e3604", "partyId-1", DeliveryMethod.DIGITAL_MAIL),
			tuple("fd99a03c-790c-4b87-bc4b-f4f73e4a2df4", "partyId-2", DeliveryMethod.DIGITAL_MAIL));

		verify(precheckServiceMock).precheckPartyIds(MUNICIPALITY_ID, request.partyIds());
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
	void checkKivraEligibility() {
		final var partyId1 = "56652549-4f96-4a8f-94f1-07d581ebbb36";
		final var partyId2 = "da03b33e-9de2-45ac-8291-31a88de59410";
		final var request = new KivraEligibilityRequest()
			.withPartyIds(List.of(partyId1, partyId2));

		when(precheckServiceMock.precheckKivra(MUNICIPALITY_ID, request)).thenReturn(List.of(partyId1, partyId2));

		final var response = webTestClient.post()
			.uri("/{municipalityId}/precheck/kivra", MUNICIPALITY_ID)
			.header(Identifier.HEADER_NAME, "type=adAccount; joe01doe")
			.contentType(APPLICATION_JSON)
			.accept(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isOk()
			.expectBody(new ParameterizedTypeReference<List<String>>() {
			})
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull().containsExactlyInAnyOrder(partyId1, partyId2);
		verify(precheckServiceMock).precheckKivra(MUNICIPALITY_ID, request);
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
