package se.sundsvall.postportalservice.integration.esigning;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.postportalservice.api.model.ESigningRequest;
import se.sundsvall.postportalservice.api.model.ESigningSignatory;
import se.sundsvall.postportalservice.integration.db.AttachmentEntity;
import se.sundsvall.postportalservice.integration.db.DepartmentEntity;
import se.sundsvall.postportalservice.integration.db.MessageEntity;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;

class EsigningMapperTest {

	private final EsigningMapper mapper = new EsigningMapper();

	@Test
	void toStartSigningRequest() {
		final var department = DepartmentEntity.create()
			.withName("Sundsvall Municipality")
			.withContactInformationEmail("dept@sundsvall.se");
		final var message = MessageEntity.create()
			.withId("msg-1")
			.withSubject("Please sign")
			.withBody("Please sign the document")
			.withDepartment(department);
		final var expires = OffsetDateTime.of(2026, 12, 31, 23, 59, 59, 0, UTC);
		final var request = ESigningRequest.create()
			.withLanguage("sv-SE")
			.withExpires(expires)
			.withSignatories(List.of(ESigningSignatory.create().withPartyId("p1").withName("John Doe").withEmail("john@sundsvall.se")));
		final var document = AttachmentEntity.create()
			.withFileName("document.pdf")
			.withContentType("application/pdf")
			.withContentString("base64content");

		final var result = mapper.toStartSigningRequest(message, request, document);

		assertThat(result.getCustomerReference()).isEqualTo("msg-1");
		assertThat(result.getLanguage()).isEqualTo("sv-SE");
		assertThat(result.getExpires()).isEqualTo(expires);
		assertThat(result.getDocument().getFileName()).isEqualTo("document.pdf");
		assertThat(result.getDocument().getMimeType()).isEqualTo("application/pdf");
		assertThat(result.getDocument().getContent()).isEqualTo("base64content");
		assertThat(result.getInitiator().getName()).isEqualTo("Sundsvall Municipality");
		assertThat(result.getInitiator().getOrganization()).isEqualTo("Sundsvall Municipality");
		assertThat(result.getInitiator().getEmail()).isEqualTo("dept@sundsvall.se");
		assertThat(result.getNotificationMessage().getSubject()).isEqualTo("Please sign");
		assertThat(result.getNotificationMessage().getBody()).isEqualTo("Please sign the document");
		assertThat(result.getSignatories()).hasSize(1);
		final var signatory = result.getSignatories().iterator().next();
		assertThat(signatory.getName()).isEqualTo("John Doe");
		assertThat(signatory.getPartyId()).isEqualTo("p1");
		assertThat(signatory.getEmail()).isEqualTo("john@sundsvall.se");
	}

	@Test
	void toInitiatorHandlesNullDepartment() {
		final var initiator = mapper.toInitiator(null);

		assertThat(initiator).isNotNull();
		assertThat(initiator.getName()).isNull();
		assertThat(initiator.getEmail()).isNull();
	}
}
