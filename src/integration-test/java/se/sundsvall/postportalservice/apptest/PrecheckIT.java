package se.sundsvall.postportalservice.apptest;

import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.postportalservice.Application;

@ActiveProfiles("it")
@WireMockAppTestSuite(files = "classpath:/PrecheckIT/", classes = Application.class)
class PrecheckIT extends AbstractAppTest {

	private static final String SERVICE_PATH = "/2281/dept44/precheck";
	private static final String RESPONSE_FILE = "response.json";
	private static final String RECIPIENTS_CSV_FILE = "recipients.csv";

	@Test
	void test01_precheck_ok() throws IOException {
		setupCall()
			.withHttpMethod(POST)
			.withServicePath(SERVICE_PATH)
			.withContentType(MULTIPART_FORM_DATA)
			.withRequestFile("file", RECIPIENTS_CSV_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_precheck_badMunicipality() throws IOException {
		setupCall()
			.withHttpMethod(POST)
			.withServicePath("/NOT_VALID/dept44/precheck")
			.withContentType(MULTIPART_FORM_DATA)
			.withRequestFile("file", RECIPIENTS_CSV_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_precheck_missingOrganizationNumber() throws IOException {
		setupCall()
			.withHttpMethod(POST)
			.withServicePath(SERVICE_PATH)
			.withContentType(MULTIPART_FORM_DATA)
			.withRequestFile("file", RECIPIENTS_CSV_FILE)
			.withExpectedResponseStatus(INTERNAL_SERVER_ERROR)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}
}
