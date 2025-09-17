package se.sundsvall.postportalservice.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static se.sundsvall.postportalservice.TestDataFactory.INVALID_MUNICIPALITY_ID;
import static se.sundsvall.postportalservice.TestDataFactory.MUNICIPALITY_ID;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.postportalservice.Application;
import se.sundsvall.postportalservice.api.model.PrecheckResponse;
import se.sundsvall.postportalservice.api.model.PrecheckResponse.DeliveryMethod;
import se.sundsvall.postportalservice.api.model.PrecheckResponse.RecipientPrecheck;
import se.sundsvall.postportalservice.service.PrecheckService;
import se.sundsvall.postportalservice.service.PrecheckService.PrecheckEntry;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class PrecheckResourceTest {

	private static final String DEPARTMENT_ID = "dept44";
	private static final String RECIPIENTS_CSV_FILE = "recipients.csv";

	@MockitoBean
	private PrecheckService precheckService;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void precheckRecipients_OK() {
		final var csv = """
			Personnummer;Kolumn2;Kolumn3
			191111111111;X;Y
			192222222222;Z;V
			""";
		final var multiparts = new MultipartBodyBuilder();

		multiparts.part("file", csv.getBytes(StandardCharsets.UTF_8))
			.filename(RECIPIENTS_CSV_FILE)
			.contentType(MediaType.TEXT_PLAIN);

		final var entries = List.of(
			new PrecheckEntry("191111111111"),
			new PrecheckEntry("192222222222"));

		final var response = PrecheckResponse.of(List.of(
			new RecipientPrecheck("191111111111", "partyId-1", DeliveryMethod.DIGITAL_MAIL, null),
			new RecipientPrecheck("192222222222", "partyId-2", DeliveryMethod.DIGITAL_MAIL, null)));

		when(precheckService.parseCsv(any())).thenReturn(entries);
		when(precheckService.precheck(eq(MUNICIPALITY_ID), eq(DEPARTMENT_ID), eq(entries))).thenReturn(response);

		webTestClient.post()
			.uri("/{municipalityId}/{departmentId}/precheck", MUNICIPALITY_ID, DEPARTMENT_ID)
			.contentType(MediaType.MULTIPART_FORM_DATA)
			.bodyValue(multiparts.build())
			.exchange()
			.expectStatus().isOk()
			.expectBody().jsonPath("$.recipients.length()").isEqualTo(2);
	}

	@Test
	void precheckRecipients_BadRequest() {
		final var csv = """
			Personnummer;Kolumn2
			191111111111;X
			""";
		final var multiparts = new MultipartBodyBuilder();

		multiparts.part("file", csv.getBytes(StandardCharsets.UTF_8))
			.filename(RECIPIENTS_CSV_FILE)
			.contentType(MediaType.TEXT_PLAIN);

		webTestClient.post()
			.uri("/{municipalityId}/{departmentId}/precheck", INVALID_MUNICIPALITY_ID, DEPARTMENT_ID)
			.contentType(MediaType.MULTIPART_FORM_DATA)
			.bodyValue(multiparts.build())
			.exchange()
			.expectStatus().isBadRequest();
	}
}
