package se.sundsvall.postportalservice.integration.messaging;

import generated.se.sundsvall.messaging.DigitalMailAttachment;
import generated.se.sundsvall.messaging.DigitalMailRequest;
import generated.se.sundsvall.messaging.SmsBatchRequest;
import generated.se.sundsvall.messaging.SmsRequest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.postportalservice.integration.db.AttachmentEntity;
import se.sundsvall.postportalservice.integration.db.DepartmentEntity;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.postportalservice.TestDataFactory.MOBILE_NUMBER;

@ExtendWith(MockitoExtension.class)
class MessagingMapperTest {

	@Test
	void toSmsRequest() {
		var departmentEntity = DepartmentEntity.create()
			.withName("DepartmentName");
		var messageEntity = MessageEntity.create()
			.withBody("Text")
			.withDepartment(departmentEntity);
		var recipientEntity = RecipientEntity.create()
			.withPhoneNumber(MOBILE_NUMBER)
			.withPartyId("PartyId");

		var result = MessagingMapper.toSmsRequest(messageEntity, recipientEntity);
		assertThat(result).isInstanceOf(SmsRequest.class);
	}

	@Test
	void toSmsBatchRequest() {
		var result = MessagingMapper.toSmsBatchRequest();
		assertThat(result).isInstanceOf(SmsBatchRequest.class);
	}

	@Test
	void toDigitalMailRequest() {
		var departmentEntity = DepartmentEntity.create()
			.withName("DepartmentName")
			.withSupportText("supportText")
			.withContactInformationEmail("contactEmail")
			.withContactInformationUrl("contactUrl")
			.withContactInformationPhoneNumber(MOBILE_NUMBER);

		var attachmentEntity = AttachmentEntity.create()
			.withContentString("contentString")
			.withContentType("application/pdf")
			.withFileName("fileName");

		var messageEntity = MessageEntity.create()
			.withBody("Text")
			.withContentType("text/plain")
			.withSubject("Subject")
			.withDepartment(departmentEntity)
			.withAttachments(List.of(attachmentEntity));

		var recipientEntity = RecipientEntity.create()
			.withPartyId("00000000-0000-0000-0000-000000000001");

		var result = MessagingMapper.toDigitalMailRequest(messageEntity, recipientEntity.getPartyId());

		assertThat(result).isNotNull();
		assertThat(result.getSender()).isNotNull();
		assertThat(result.getSender().getSupportInfo()).satisfies(supportInfo -> {
			assertThat(supportInfo.getText()).isEqualTo("supportText");
			assertThat(supportInfo.getEmailAddress()).isEqualTo("contactEmail");
			assertThat(supportInfo.getPhoneNumber()).isEqualTo(MOBILE_NUMBER);
			assertThat(supportInfo.getUrl()).isEqualTo("contactUrl");
		});
		assertThat(result.getAttachments()).isNotNull();
		assertThat(result.getAttachments()).allSatisfy(attachment -> {
			assertThat(attachment.getFilename()).isEqualTo(attachmentEntity.getFileName());
			assertThat(attachment.getContentType()).isEqualTo(DigitalMailAttachment.ContentTypeEnum.APPLICATION_PDF);
			assertThat(attachment.getContent()).isEqualTo("contentString");
		});
		assertThat(result).satisfies(digitalMailRequest -> {
			assertThat(digitalMailRequest.getSubject()).isEqualTo(messageEntity.getSubject());
			assertThat(digitalMailRequest.getBody()).isEqualTo(messageEntity.getBody());
			assertThat(digitalMailRequest.getContentType()).isEqualTo(DigitalMailRequest.ContentTypeEnum.TEXT_PLAIN);
			assertThat(digitalMailRequest.getDepartment()).isEqualTo(departmentEntity.getName());
		});
		assertThat(result.getParty()).isNotNull().satisfies(party -> {
			assertThat(party.getPartyIds().getFirst()).isEqualTo(UUID.fromString(recipientEntity.getPartyId()));
		});
	}

	@Test
	void toDigitalMailAttachments() {
		var attachmentEntity = AttachmentEntity.create()
			.withContentString("contentString")
			.withContentType("application/pdf")
			.withFileName("fileName");

		var attachments = List.of(attachmentEntity, attachmentEntity);

		var result = MessagingMapper.toDigitalMailAttachments(attachments);

		assertThat(result).isNotNull();
		assertThat(result).hasSize(2);
		assertThat(result).allSatisfy(attachment -> {
			assertThat(attachment.getFilename()).isEqualTo(attachmentEntity.getFileName());
			assertThat(attachment.getContentType()).isEqualTo(DigitalMailAttachment.ContentTypeEnum.APPLICATION_PDF);
			assertThat(attachment.getContent()).isEqualTo("contentString");
		});
	}

	@Test
	void toDigitalMailAttachment() {
		var attachmentEntity = AttachmentEntity.create()
			.withContentString("contentString")
			.withContentType("application/pdf")
			.withFileName("fileName");

		var result = MessagingMapper.toDigitalMailAttachment(attachmentEntity);

		assertThat(result).isNotNull();
		assertThat(result.getFilename()).isEqualTo(attachmentEntity.getFileName());
		assertThat(result.getContentType()).isEqualTo(DigitalMailAttachment.ContentTypeEnum.APPLICATION_PDF);
		assertThat(result.getContent()).isEqualTo("contentString");
	}
}
