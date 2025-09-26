package se.sundsvall.postportalservice.integration.messaging;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.ObjectUtils.anyNull;

import generated.se.sundsvall.messaging.Address;
import generated.se.sundsvall.messaging.DigitalMailAttachment;
import generated.se.sundsvall.messaging.DigitalMailParty;
import generated.se.sundsvall.messaging.DigitalMailRequest;
import generated.se.sundsvall.messaging.DigitalMailSender;
import generated.se.sundsvall.messaging.DigitalMailSenderSupportInfo;
import generated.se.sundsvall.messaging.SmsBatchRequest;
import generated.se.sundsvall.messaging.SmsRequest;
import generated.se.sundsvall.messaging.SmsRequestParty;
import generated.se.sundsvall.messaging.SnailmailAttachment;
import generated.se.sundsvall.messaging.SnailmailRequest;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import se.sundsvall.postportalservice.integration.db.AttachmentEntity;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.service.util.BlobUtil;

public final class MessagingMapper {

	private MessagingMapper() {

	}

	public static SmsRequest toSmsRequest(final MessageEntity messageEntity, final RecipientEntity recipientEntity) {
		if (anyNull(messageEntity, recipientEntity)) {
			return null;
		}
		return new SmsRequest()
			.message(messageEntity.getBody())
			.department(messageEntity.getDepartment().getName())
			.mobileNumber(recipientEntity.getPhoneNumber())
			.party(new SmsRequestParty().partyId(recipientEntity.getPartyId()));
	}

	public static SmsBatchRequest toSmsBatchRequest() {
		return new SmsBatchRequest();
	}

	public static DigitalMailRequest toDigitalMailRequest(final MessageEntity messageEntity, final String partyId) {
		return Optional.ofNullable(messageEntity).map(present -> new DigitalMailRequest()
			.contentType(DigitalMailRequest.ContentTypeEnum.fromValue(messageEntity.getContentType()))
			.body(messageEntity.getBody())
			.subject(messageEntity.getSubject())
			.department(messageEntity.getDepartment().getName())
			.party(new DigitalMailParty().partyIds(List.of(UUID.fromString(partyId))))
			.attachments(toDigitalMailAttachments(messageEntity.getAttachments()))
			.sender(new DigitalMailSender().supportInfo(new DigitalMailSenderSupportInfo()
				.emailAddress(messageEntity.getDepartment().getContactInformationEmail())
				.phoneNumber(messageEntity.getDepartment().getContactInformationPhoneNumber())
				.url(messageEntity.getDepartment().getContactInformationUrl())
				.text(messageEntity.getDepartment().getSupportText()))))
			.orElse(null);
	}

	public static List<DigitalMailAttachment> toDigitalMailAttachments(final List<AttachmentEntity> attachmentEntities) {
		return Optional.ofNullable(attachmentEntities).orElse(emptyList()).stream()
			.map(MessagingMapper::toDigitalMailAttachment)
			.filter(Objects::nonNull)
			.toList();
	}

	public static DigitalMailAttachment toDigitalMailAttachment(final AttachmentEntity attachmentEntity) {
		return Optional.ofNullable(attachmentEntity).map(present -> new DigitalMailAttachment()
			.filename(attachmentEntity.getFileName())
			.content(BlobUtil.convertBlobToBase64String(attachmentEntity.getContent()))
			.contentType(DigitalMailAttachment.ContentTypeEnum.fromValue(attachmentEntity.getContentType())))
			.orElse(null);
	}

	public static SnailmailRequest toSnailmailRequest(final MessageEntity messageEntity, final RecipientEntity recipientEntity) {
		if (anyNull(messageEntity, recipientEntity)) {
			return null;
		}
		return new SnailmailRequest()
			.address(toAddress(recipientEntity))
			.attachments(toSnailmailAttachments(messageEntity.getAttachments()))
			.department(messageEntity.getDepartment().getName());
	}

	public static List<SnailmailAttachment> toSnailmailAttachments(final List<AttachmentEntity> attachmentEntities) {
		return Optional.ofNullable(attachmentEntities).orElse(emptyList()).stream()
			.map(MessagingMapper::toSnailmailAttachment)
			.filter(Objects::nonNull)
			.toList();
	}

	public static SnailmailAttachment toSnailmailAttachment(final AttachmentEntity attachmentEntity) {
		return Optional.ofNullable(attachmentEntity).map(present -> new SnailmailAttachment()
			.filename(attachmentEntity.getFileName())
			.content(BlobUtil.convertBlobToBase64String(attachmentEntity.getContent()))
			.contentType(attachmentEntity.getContentType()))
			.orElse(null);
	}

	public static Address toAddress(final RecipientEntity recipientEntity) {
		return Optional.ofNullable(recipientEntity).map(present -> new Address()
			.address(recipientEntity.getStreetAddress())
			.apartmentNumber(recipientEntity.getApartmentNumber())
			.careOf(recipientEntity.getCareOf())
			.city(recipientEntity.getCity())
			.country(recipientEntity.getCountry())
			.firstName(recipientEntity.getFirstName())
			.lastName(recipientEntity.getLastName())
			.zipCode(recipientEntity.getZipCode()))
			.orElse(null);
	}

}
