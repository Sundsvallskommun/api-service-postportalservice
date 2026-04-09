package se.sundsvall.postportalservice.service.mapper;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import se.sundsvall.postportalservice.integration.db.AttachmentEntity;
import se.sundsvall.postportalservice.integration.db.DepartmentEntity;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.db.UserEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.CONTACT_INFORMATION_EMAIL;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.CONTACT_INFORMATION_PHONE_NUMBER;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.CONTACT_INFORMATION_URL;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.ORGANIZATION_NUMBER;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.SUPPORT_TEXT;

class RecipientMapperTest {

	private final RecipientMapper mapper = new RecipientMapper();

	@Test
	void toRecipientResponse() {
		final var recipient = RecipientEntity.create()
			.withId("recipientId")
			.withPartyId("partyId");

		final var attachment = AttachmentEntity.create()
			.withId("attachmentId")
			.withFileName("file.pdf")
			.withContentType("application/pdf");

		final var message = MessageEntity.create()
			.withSubject("subject")
			.withBody("body")
			.withContentType("text/html")
			.withUser(new UserEntity().withUsername("joe01doe"))
			.withDepartment(DepartmentEntity.create().withName("Department 44"))
			.withRecipients(List.of(recipient))
			.withAttachments(List.of(attachment));

		final var settingsMap = Map.of(
			ORGANIZATION_NUMBER, "1234567890",
			SUPPORT_TEXT, "Support text",
			CONTACT_INFORMATION_URL, "https://example.com",
			CONTACT_INFORMATION_EMAIL, "test@example.com",
			CONTACT_INFORMATION_PHONE_NUMBER, "+46123456789");

		final var result = mapper.toRecipientResponse(message, recipient, settingsMap);

		assertThat(result.partyId()).isEqualTo("partyId");
		assertThat(result.subject()).isEqualTo("subject");
		assertThat(result.body()).isEqualTo("body");
		assertThat(result.contentType()).isEqualTo("text/html");
		assertThat(result.organizationNumber()).isEqualTo("1234567890");
		assertThat(result.organizationName()).isEqualTo("Department 44");
		assertThat(result.supportText()).isEqualTo("Support text");
		assertThat(result.contactInformationUrl()).isEqualTo("https://example.com");
		assertThat(result.contactInformationEmail()).isEqualTo("test@example.com");
		assertThat(result.contactInformationPhoneNumber()).isEqualTo("+46123456789");
		assertThat(result.identifier()).isEqualTo("joe01doe");
		assertThat(result.attachments()).hasSize(1).first().satisfies(a -> {
			assertThat(a.id()).isEqualTo("attachmentId");
			assertThat(a.fileName()).isEqualTo("file.pdf");
			assertThat(a.contentType()).isEqualTo("application/pdf");
		});
	}

	@Test
	void toAttachmentMetadata() {
		final var attachment = AttachmentEntity.create()
			.withId("id")
			.withFileName("doc.pdf")
			.withContentType("application/pdf");

		final var result = mapper.toAttachmentMetadata(attachment);

		assertThat(result.id()).isEqualTo("id");
		assertThat(result.fileName()).isEqualTo("doc.pdf");
		assertThat(result.contentType()).isEqualTo("application/pdf");
	}
}
