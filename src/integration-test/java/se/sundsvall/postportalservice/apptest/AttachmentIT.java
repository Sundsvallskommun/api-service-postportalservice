package se.sundsvall.postportalservice.apptest;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.postportalservice.Application;

@Sql(scripts = {
	"/db/script/truncate.sql",
	"/db/script/testdata.sql"
})
@WireMockAppTestSuite(files = "classpath:/AttachmentIT/", classes = Application.class)
class AttachmentIT extends AbstractAppTest {

	private static final String FILE_NAME = "attachment.pdf";

	@Test
	void test01_downloadAttachment() throws IOException {
		setupCall()
			.withServicePath("/2281/attachments/5a70a27f-997e-431e-9155-cc50d01e80c5")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_PDF_VALUE))
			.withExpectedBinaryResponse(FILE_NAME)
			.sendRequestAndVerifyResponse();
	}
}
