package se.sundsvall.postportalservice.service.mapper;

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;
import se.sundsvall.postportalservice.api.model.Message;
import se.sundsvall.postportalservice.api.model.MessageDetails;
import se.sundsvall.postportalservice.integration.db.AttachmentEntity;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;

@Component
public class HistoryMapper {

	public List<Message> toMessageList(final List<MessageEntity> messageEntities) {
		return Optional.ofNullable(messageEntities).orElse(emptyList()).stream()
			.map(this::toMessage)
			.filter(Objects::nonNull)
			.toList();
	}

	public Message toMessage(final MessageEntity messageEntity) {
		return Optional.ofNullable(messageEntity).map(present -> Message.create()
			.withMessageId(messageEntity.getId())
			.withType(messageEntity.getMessageType().toString())
			.withSubject(messageEntity.getSubject())
			.withSentAt(messageEntity.getCreated().toLocalDateTime()))
			.orElse(null);
	}

	public MessageDetails toMessageDetails(final MessageEntity messageEntity) {
		return Optional.ofNullable(messageEntity).map(present -> MessageDetails.create()
			.withSubject(messageEntity.getSubject())
			.withSentAt(messageEntity.getCreated().toLocalDateTime())
			.withAttachments(toAttachmentList(messageEntity.getAttachments()))
			.withRecipients(toRecipientList(messageEntity.getRecipients())))
			.orElse(null);
	}

	public List<MessageDetails.Attachment> toAttachmentList(final List<AttachmentEntity> attachments) {
		return Optional.ofNullable(attachments).orElse(emptyList()).stream()
			.map(this::toAttachment)
			.filter(Objects::nonNull)
			.toList();
	}

	public MessageDetails.Attachment toAttachment(final AttachmentEntity attachmentEntity) {
		return Optional.ofNullable(attachmentEntity).map(present -> MessageDetails.Attachment.create()
			.withFileName(attachmentEntity.getFileName())
			.withContentType(attachmentEntity.getContentType())
			.withAttachmentId(attachmentEntity.getId()))
			.orElse(null);
	}

	public List<MessageDetails.Recipient> toRecipientList(final List<RecipientEntity> recipientEntities) {
		return Optional.ofNullable(recipientEntities).orElse(emptyList()).stream()
			.map(this::toRecipient)
			.filter(Objects::nonNull)
			.toList();
	}

	public MessageDetails.Recipient toRecipient(final RecipientEntity recipientEntity) {
		return Optional.ofNullable(recipientEntity).map(present -> MessageDetails.Recipient.create()
			.withPartyId(recipientEntity.getPartyId())
			.withCity(recipientEntity.getCity())
			.withStreetAddress(recipientEntity.getStreetAddress())
			.withMobileNumber(recipientEntity.getPhoneNumber())
			.withName("%s %s".formatted(recipientEntity.getFirstName(), recipientEntity.getLastName()))
			.withMessageType(recipientEntity.getMessageType().toString())
			.withZipCode(recipientEntity.getZipCode()))
			.orElse(null);
	}

}
