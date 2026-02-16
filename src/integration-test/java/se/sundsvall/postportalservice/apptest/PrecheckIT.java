package se.sundsvall.postportalservice.apptest;

import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

import java.io.FileNotFoundException;
import org.junit.jupiter.api.Test;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.postportalservice.Application;

@WireMockAppTestSuite(files = "classpath:/PrecheckIT/", classes = Application.class)
class PrecheckIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";

	@Test
	void test01_precheck_ok() {
		setupCall()
			.withHttpMethod(POST)
			.withServicePath("/2281/precheck")
			.withHeader(Identifier.HEADER_NAME, "type=adAccount; joe01doe")
			.withContentType(APPLICATION_JSON)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_precheck_kivra() {
		setupCall()
			.withHttpMethod(POST)
			.withServicePath("/2281/precheck/kivra")
			.withHeader(Identifier.HEADER_NAME, "type=adAccount; joe01doe")
			.withContentType(APPLICATION_JSON)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_precheck_csv_valid_format() throws FileNotFoundException {
		setupCall()
			.withServicePath("/2281/precheck/csv")
			.withHttpMethod(POST)
			.withContentType(MULTIPART_FORM_DATA)
			.withRequestFile("csv-file", "legalIds.csv")
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_precheck_csv_invalid_value() throws FileNotFoundException {
		setupCall()
			.withServicePath("/2281/precheck/csv")
			.withHttpMethod(POST)
			.withContentType(MULTIPART_FORM_DATA)
			.withRequestFile("csv-file", "legalIds.csv")
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_precheck_csv_invalid_header() throws FileNotFoundException {
		setupCall()
			.withServicePath("/2281/precheck/csv")
			.withHttpMethod(POST)
			.withContentType(MULTIPART_FORM_DATA)
			.withRequestFile("csv-file", "legalIds.csv")
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test06_precheck_with_minor() {
		setupCall()
			.withHttpMethod(POST)
			.withServicePath("/2281/precheck")
			.withHeader(Identifier.HEADER_NAME, "type=adAccount; joe01doe")
			.withContentType(APPLICATION_JSON)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test07_precheck_csv_no_party_ids() throws FileNotFoundException {
		setupCall()
			.withServicePath("/2281/precheck/csv")
			.withHttpMethod(POST)
			.withContentType(MULTIPART_FORM_DATA)
			.withRequestFile("csv-file", "legalIds.csv")
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test08_precheck_csv_partial_party_ids() throws FileNotFoundException {
		setupCall()
			.withServicePath("/2281/precheck/csv")
			.withHttpMethod(POST)
			.withContentType(MULTIPART_FORM_DATA)
			.withRequestFile("csv-file", "legalIds.csv")
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}
}
