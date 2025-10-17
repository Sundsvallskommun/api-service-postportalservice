package se.sundsvall.postportalservice.service.mapper;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static se.sundsvall.postportalservice.service.util.StringUtil.calculateFullName;

import generated.se.sundsvall.digitalregisteredletter.LetterStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import se.sundsvall.postportalservice.api.model.Message;
import se.sundsvall.postportalservice.api.model.MessageDetails;
import se.sundsvall.postportalservice.api.model.SigningStatus;
import se.sundsvall.postportalservice.integration.db.AttachmentEntity;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.db.converter.MessageType;

@Component
public class HistoryMapper {

	public List<Message> toMessageList(final List<MessageEntity> messageEntities) {
		return ofNullable(messageEntities).orElse(emptyList()).stream()
			.map(this::toMessage)
			.filter(Objects::nonNull)
			.toList();
	}

	public Message toMessage(final MessageEntity nullableMessageEntity) {
		return ofNullable(nullableMessageEntity)
			.map(messageEntity -> Message.create()
				.withMessageId(messageEntity.getId())
				.withNumberOfRecipients(ofNullable(messageEntity.getRecipients()).map(List::size).orElse(0))
				.withType(ofNullable(messageEntity.getMessageType()).map(MessageType::name).orElse(null))
				.withSubject(messageEntity.getSubject())
				.withSentAt(ofNullable(messageEntity.getCreated()).map(OffsetDateTime::toLocalDateTime).orElse(null)))
			.orElse(null);
	}

	public MessageDetails toMessageDetails(final MessageEntity nullableMessageEntity) {
		return ofNullable(nullableMessageEntity)
			.map(messageEntity -> MessageDetails.create()
				.withSubject(messageEntity.getSubject())
				.withSentAt(ofNullable(messageEntity.getCreated()).map(OffsetDateTime::toLocalDateTime).orElse(null))
				.withAttachments(toAttachmentList(messageEntity.getAttachments()))
				.withRecipients(toRecipientList(messageEntity.getRecipients())))
			.orElse(null);
	}

	public List<MessageDetails.AttachmentDetails> toAttachmentList(final List<AttachmentEntity> attachments) {
		return ofNullable(attachments).orElse(emptyList()).stream()
			.map(this::toAttachment)
			.filter(Objects::nonNull)
			.toList();
	}

	public MessageDetails.AttachmentDetails toAttachment(final AttachmentEntity nullableAttachmentEntity) {
		return ofNullable(nullableAttachmentEntity)
			.map(attachmentEntity -> MessageDetails.AttachmentDetails.create()
				.withFileName(attachmentEntity.getFileName())
				.withContentType(attachmentEntity.getContentType())
				.withAttachmentId(attachmentEntity.getId()))
			.orElse(null);
	}

	public List<MessageDetails.RecipientDetails> toRecipientList(final List<RecipientEntity> recipientEntities) {
		return ofNullable(recipientEntities).orElse(emptyList()).stream()
			.map(this::toRecipient)
			.filter(Objects::nonNull)
			.toList();
	}

	public MessageDetails.RecipientDetails toRecipient(final RecipientEntity nullableRecipientEntity) {
		return ofNullable(nullableRecipientEntity)
			.map(recipientEntity -> MessageDetails.RecipientDetails.create()
				.withPartyId(recipientEntity.getPartyId())
				.withCity(recipientEntity.getCity())
				.withStreetAddress(recipientEntity.getStreetAddress())
				.withMobileNumber(recipientEntity.getPhoneNumber())
				.withName(calculateFullName(recipientEntity.getFirstName(), recipientEntity.getLastName()))
				.withMessageType(ofNullable(recipientEntity.getMessageType()).map(MessageType::name).orElse(null))
				.withStatus(recipientEntity.getStatus())
				.withZipCode(recipientEntity.getZipCode()))
			.orElse(null);
	}

	public SigningStatus toSigningStatus(LetterStatus nullableLetterStatus) {
		return ofNullable(nullableLetterStatus)
			.map(letterStatus -> SigningStatus.create()
				.withLetterState(letterStatus.getStatus())
				.withSigningProcessState(letterStatus.getSigningInformation()))
			.orElse(null);
	}

}
