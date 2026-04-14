package se.sundsvall.postportalservice.apptest;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.postportalservice.Application;
import se.sundsvall.postportalservice.apptest.rabbitmq.RabbitMQContainerInitializer;

@Sql(scripts = {
	"/db/script/truncate.sql",
	"/db/script/testdata.sql"
})
@WireMockAppTestSuite(files = "classpath:/StatisticsIT/", classes = Application.class)
@ContextConfiguration(initializers = RabbitMQContainerInitializer.class)
class StatisticsIT extends AbstractAppTest {

	private static final String RESPONSE_FILE = "response.json";

	@Test
	void test01_getStatistics_2025_september() {
		setupCall()
			.withServicePath(uriBuilder -> uriBuilder.replacePath("/2281/statistics/departments")
				.replaceQueryParam("year", "2025")
				.replaceQueryParam("month", "9")
				.build())
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_getStatistics_2025_august() {
		setupCall()
			.withServicePath(uriBuilder -> uriBuilder.replacePath("/2281/statistics/departments")
				.replaceQueryParam("year", "2025")
				.replaceQueryParam("month", "08")
				.build())
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

}
