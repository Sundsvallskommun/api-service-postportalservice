package se.sundsvall.postportalservice.integration.db.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.postportalservice.api.model.Statistics;

/**
 * StatisticsRepository tests.
 *
 * @see "/src/test/resources/db/script/testdata.sql for data setup"
 */
@Sql(scripts = {
	"/db/script/testdata.sql"
})
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Import(StatisticsRepository.class)
class StatisticsRepositoryTest {

	@Autowired
	private StatisticsRepository statisticsRepository;

	@Test
	void getDepartmentStatistics_september_2025() {
		final var year = "2025";
		final var month = "9";

		final var result = statisticsRepository.getDepartmentStatisticsByYearAndMonth(year, month);

		assertThat(result).hasSize(5)
			.extracting(
				Statistics::getId,
				Statistics::getName,
				Statistics::getSnailMail,
				Statistics::getDigitalMail,
				Statistics::getDigitalRegisteredLetter,
				Statistics::getSms)
			.containsExactlyInAnyOrder(
				tuple("9a8b6e67-6007-4379-a717-cca245448400", "Miljöförvaltningen", 3L, 0L, 0L, 0L),
				tuple("7b137896-cc1d-479b-bf2f-fc663eb8b943", "Socialförvaltningen", 0L, 2L, 0L, 1L),
				tuple("e3e146fb-aac9-467c-a19a-c90ee82caed4", "IT-avdelningen", 0L, 0L, 1L, 0L),
				tuple("0072f95f-c1fa-426a-87e9-adb8e0112bf1", "HR-avdelningen", 0L, 0L, 0L, 3L),
				tuple("e9c2ebba-4b71-4cc1-bc56-46434f8693cc", "Kulturförvaltningen", 1L, 1L, 0L, 1L));
	}

	@Test
	void getDepartmentStatistics_august_2025() {
		final var year = "2025";
		final var month = "8";

		final var result = statisticsRepository.getDepartmentStatisticsByYearAndMonth(year, month);

		assertThat(result).hasSize(2).extracting(
			Statistics::getId,
			Statistics::getName,
			Statistics::getSnailMail,
			Statistics::getDigitalMail,
			Statistics::getDigitalRegisteredLetter,
			Statistics::getSms).containsExactlyInAnyOrder(
				tuple("9a8b6e67-6007-4379-a717-cca245448400", "Miljöförvaltningen", 25L, 5L, 0L, 5L),
				tuple("7b137896-cc1d-479b-bf2f-fc663eb8b943", "Socialförvaltningen", 0L, 20L, 10L, 5L));
	}

	@Test
	void statisticsMapper_mapRow() throws SQLException {
		final var departmentId = "123";
		final var departmentName = "Test Department";
		final var snailMailCount = 10L;
		final var digitalMailCount = 20L;
		final var digitalRegisteredLetterCount = 5L;
		final var smsCount = 15L;

		final var resultSetMock = Mockito.mock(ResultSet.class);
		when(resultSetMock.getString("department_id")).thenReturn(departmentId);
		when(resultSetMock.getString("department_name")).thenReturn(departmentName);
		when(resultSetMock.getLong("snail_mail_count")).thenReturn(snailMailCount);
		when(resultSetMock.getLong("digital_mail_count")).thenReturn(digitalMailCount);
		when(resultSetMock.getLong("digital_registered_letter_count")).thenReturn(digitalRegisteredLetterCount);
		when(resultSetMock.getLong("sms_count")).thenReturn(smsCount);

		final var mapper = new StatisticsRepository.StatisticsMapper();

		final var result = mapper.mapRow(resultSetMock, 3);

		assertThat(result).isNotNull().satisfies(statistics -> {
			assertThat(statistics.getId()).isEqualTo(departmentId);
			assertThat(statistics.getName()).isEqualTo(departmentName);
			assertThat(statistics.getSnailMail()).isEqualTo(snailMailCount);
			assertThat(statistics.getDigitalMail()).isEqualTo(digitalMailCount);
			assertThat(statistics.getDigitalRegisteredLetter()).isEqualTo(digitalRegisteredLetterCount);
			assertThat(statistics.getSms()).isEqualTo(smsCount);
		});

		verify(resultSetMock).getString("department_id");
		verify(resultSetMock).getString("department_name");
		verify(resultSetMock).getLong("snail_mail_count");
		verify(resultSetMock).getLong("digital_mail_count");
		verify(resultSetMock).getLong("digital_registered_letter_count");
		verify(resultSetMock).getLong("sms_count");
	}

}
