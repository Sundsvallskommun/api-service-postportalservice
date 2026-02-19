package se.sundsvall.postportalservice.api;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;
import se.sundsvall.postportalservice.Application;
import se.sundsvall.postportalservice.service.StatisticsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static se.sundsvall.postportalservice.TestDataFactory.INVALID_MUNICIPALITY_ID;
import static se.sundsvall.postportalservice.TestDataFactory.MUNICIPALITY_ID;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
@AutoConfigureWebTestClient
class StatisticsResourceTest {

	@MockitoBean
	private StatisticsService statisticsServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void getStatisticsByDepartment_OK() {
		final var year = "2025";
		final var month = "12";

		webTestClient.get()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/statistics/departments")
				.queryParam("year", year)
				.queryParam("month", month)
				.build(MUNICIPALITY_ID))
			.exchange()
			.expectStatus().isOk();

		verify(statisticsServiceMock).getDepartmentStatistics(year, month);

		verifyNoMoreInteractions(statisticsServiceMock);
	}

	@Test
	void getStatisticsByDepartment_BadPath() {
		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/statistics/departments")
				.queryParam("year", "2025")
				.queryParam("month", "12")
				.build(INVALID_MUNICIPALITY_ID))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getViolations()).hasSize(1).satisfiesExactly(violation -> {
			assertThat(violation.field()).isEqualTo("getStatisticsByDepartment.municipalityId");
			assertThat(violation.message()).isEqualTo("not a valid municipality ID");
		});

		verifyNoInteractions(statisticsServiceMock);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("getStatisticsByDepartmentMissingParametersProvider")
	void getStatisticsByDepartment_MissingParameters(String testDescription, MultiValueMap<String, String> parameters, String expectedDetail) {
		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/statistics/departments")
				.queryParams(parameters)
				.build(MUNICIPALITY_ID))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response.getTitle()).isEqualTo("Bad Request");
		assertThat(response.getDetail()).isEqualTo(expectedDetail);

		verifyNoInteractions(statisticsServiceMock);
	}

	private static Stream<Arguments> getStatisticsByDepartmentMissingParametersProvider() {
		return Stream.of(
			Arguments.of("Missing year parameter", new LinkedMultiValueMap<>(Map.of("month", List.of("12"))), "Required parameter 'year' is not present."),
			Arguments.of("Missing month parameter", new LinkedMultiValueMap<>(Map.of("year", List.of("9999"))), "Required parameter 'month' is not present."));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("getStatisticsByDepartmentBadParametersProvider")
	void getStatisticsByDepartment_BadParameters(String testDescription, MultiValueMap<String, String> parameters, List<Violation> expectedViolations) {
		final var response = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.replacePath("/{municipalityId}/statistics/departments")
				.queryParams(parameters)
				.build(MUNICIPALITY_ID))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getViolations()).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedViolations);

		verifyNoInteractions(statisticsServiceMock);
	}

	private static Stream<Arguments> getStatisticsByDepartmentBadParametersProvider() {
		return Stream.of(
			Arguments.of("Outside lower bound for parameters", new LinkedMultiValueMap<>(Map.of("year", List.of("-1"), "month", List.of("0"))), List.of(
				new Violation("getStatisticsByDepartment.month", "must be a value between 1 and 12"),
				new Violation("getStatisticsByDepartment.year", "must be a value between 0 and 9999"))),

			Arguments.of("Outside upper bound for parameters", new LinkedMultiValueMap<>(Map.of("year", List.of("10000"), "month", List.of("13"))), List.of(
				new Violation("getStatisticsByDepartment.month", "must be a value between 1 and 12"),
				new Violation("getStatisticsByDepartment.year", "must be a value between 0 and 9999"))));
	}

}
