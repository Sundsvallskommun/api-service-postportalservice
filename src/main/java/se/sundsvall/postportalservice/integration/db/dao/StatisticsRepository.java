package se.sundsvall.postportalservice.integration.db.dao;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import se.sundsvall.postportalservice.api.model.Statistics;

@Repository
public class StatisticsRepository {

	private final NamedParameterJdbcTemplate jdbcTemplate;
	private final StatisticsMapper statisticsMapper = new StatisticsMapper();

	public StatisticsRepository(final NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@CircuitBreaker(name = "statisticsRepository")
	public List<Statistics> getDepartmentStatisticsByYearAndMonth(final String year, final String month) {
		var parameters = Map.of("year", year, "month", month);
		var sql = """
			SELECT department_id, department_name, snail_mail_count, digital_mail_count, sms_count, digital_registered_letter_count
			FROM v_department_monthly_statistics stats
			WHERE stats.month = :month AND stats.year = :year
			""";

		return jdbcTemplate.query(sql, parameters, statisticsMapper);
	}

	static class StatisticsMapper implements RowMapper<Statistics> {

		@Override
		public Statistics mapRow(final ResultSet resultSet, final int rowNum) throws SQLException {
			return Statistics.create()
				.withId(resultSet.getString("department_id"))
				.withName(resultSet.getString("department_name"))
				.withSnailMail(resultSet.getLong("snail_mail_count"))
				.withDigitalMail(resultSet.getLong("digital_mail_count"))
				.withSms(resultSet.getLong("sms_count"))
				.withDigitalRegisteredLetter(resultSet.getLong("digital_registered_letter_count"));
		}
	}

}
