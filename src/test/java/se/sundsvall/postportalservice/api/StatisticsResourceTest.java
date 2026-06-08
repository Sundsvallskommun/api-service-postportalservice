package se.sundsvall.postportalservice.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.postportalservice.Application;
import se.sundsvall.postportalservice.service.StatisticsService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
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

}
