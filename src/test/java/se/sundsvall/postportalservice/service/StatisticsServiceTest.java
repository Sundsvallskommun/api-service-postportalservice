package se.sundsvall.postportalservice.service;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.postportalservice.api.model.Statistics;
import se.sundsvall.postportalservice.integration.db.dao.StatisticsRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

	@Mock
	private StatisticsRepository statisticsRepository;

	@InjectMocks
	private StatisticsService statisticsService;

	@AfterEach
	void ensureNoUnexpectedMockInteractions() {
		verifyNoMoreInteractions(statisticsRepository);
	}

	@Test
	void getDepartmentStatistics() {
		var year = "2025";
		var month = "9";
		var statistics = Statistics.create()
			.withId("random-id")
			.withName("Test Department")
			.withDigitalMail(5L)
			.withSms(12L)
			.withDigitalRegisteredLetter(1337L)
			.withSnailMail(35L);
		var statisticsList = List.of(statistics);

		when(statisticsRepository.getDepartmentStatisticsByYearAndMonth(year, month)).thenReturn(statisticsList);

		var result = statisticsService.getDepartmentStatistics(year, month);

		assertThat(result).isNotNull().hasSize(1).allSatisfy(statistics1 -> {
			assertThat(statistics1.getId()).isEqualTo(statistics.getId());
			assertThat(statistics1.getName()).isEqualTo(statistics.getName());
			assertThat(statistics1.getDigitalMail()).isEqualTo(statistics.getDigitalMail());
			assertThat(statistics1.getSms()).isEqualTo(statistics.getSms());
			assertThat(statistics1.getDigitalRegisteredLetter()).isEqualTo(statistics.getDigitalRegisteredLetter());
			assertThat(statistics1.getSnailMail()).isEqualTo(statistics.getSnailMail());
		});

		verify(statisticsRepository).getDepartmentStatisticsByYearAndMonth(year, month);
	}

}
