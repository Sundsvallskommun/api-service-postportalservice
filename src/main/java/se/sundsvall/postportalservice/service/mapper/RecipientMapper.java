package se.sundsvall.postportalservice.service.mapper;

import java.util.Map;
import org.springframework.stereotype.Component;
import se.sundsvall.postportalservice.api.model.RecipientResponse;
import se.sundsvall.postportalservice.api.model.RecipientResponse.AttachmentMetadata;
import se.sundsvall.postportalservice.integration.db.AttachmentEntity;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;

import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.CONTACT_INFORMATION_EMAIL;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.CONTACT_INFORMATION_PHONE_NUMBER;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.CONTACT_INFORMATION_URL;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.ORGANIZATION_NUMBER;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.SUPPORT_TEXT;

@Component
public class RecipientMapper {

	public RecipientResponse toRecipientResponse(final MessageEntity message, final RecipientEntity recipient, final Map<String, String> settingsMap) {
		return new RecipientResponse(
			recipient.getPartyId(),
			message.getSubject(),
			message.getBody(),
			message.getContentType(),
			settingsMap.get(ORGANIZATION_NUMBER),
			message.getDepartment().getName(),
			settingsMap.get(SUPPORT_TEXT),
			settingsMap.get(CONTACT_INFORMATION_URL),
			settingsMap.get(CONTACT_INFORMATION_EMAIL),
			settingsMap.get(CONTACT_INFORMATION_PHONE_NUMBER),
			message.getUser().getUsername(),
			message.getAttachments().stream()
				.map(this::toAttachmentMetadata)
				.toList());
	}

	public AttachmentMetadata toAttachmentMetadata(final AttachmentEntity attachmentEntity) {
		return new AttachmentMetadata(attachmentEntity.getId(), attachmentEntity.getFileName(), attachmentEntity.getContentType());
	}
}
