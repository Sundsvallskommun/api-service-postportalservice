package se.sundsvall.postportalservice.integration.messaging;

import generated.se.sundsvall.messaging.Address;
import generated.se.sundsvall.messaging.DigitalMailAttachment;
import generated.se.sundsvall.messaging.DigitalMailParty;
import generated.se.sundsvall.messaging.DigitalMailRequest;
import generated.se.sundsvall.messaging.DigitalMailSender;
import generated.se.sundsvall.messaging.DigitalMailSenderSupportInfo;
import generated.se.sundsvall.messaging.EmailAttachment;
import generated.se.sundsvall.messaging.EmailRequest;
import generated.se.sundsvall.messaging.EmailRequestParty;
import generated.se.sundsvall.messaging.EmailSender;
import generated.se.sundsvall.messaging.SmsBatchRequest;
import generated.se.sundsvall.messaging.SmsRequest;
import generated.se.sundsvall.messaging.SmsRequestParty;
import generated.se.sundsvall.messaging.SnailmailAttachment;
import generated.se.sundsvall.messaging.SnailmailParty;
import generated.se.sundsvall.messaging.SnailmailRequest;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.postportalservice.integration.db.AttachmentEntity;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.ObjectUtils.anyNull;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.SNAILMAIL_CALLBACK_EMAIL;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.SNAILMAIL_CALLBACK_SUBJECT;

public final class MessagingMapper {

	// Extremely temporary, will use templating in the future
	private static final String MAIL_CONTENT = """
		Bifogat är ett brev som ej kunnat levereras som digital post och måste skickas som fysisk post av er, skicka till:
		Namn: %s %s
		Adress: %s
		c/o: %s
		Postnummer: %s
		Ort: %s
		""";
	private static final String EMAIL_SENDER_NAME = "Postportalen";
	private static final String EMAIL_SENDER_ADDRESS = "noreply@postportal.se";

	private MessagingMapper() {}

	public static SmsRequest toSmsRequest(final MessageEntity messageEntity, final RecipientEntity recipientEntity) {
		if (anyNull(messageEntity, recipientEntity)) {
			return null;
		}
		return new SmsRequest()
			.sender(messageEntity.getDisplayName())
			.message(messageEntity.getBody())
			.department(messageEntity.getDepartment().getName())
			.mobileNumber(recipientEntity.getPhoneNumber())
			.party(new SmsRequestParty().partyId(recipientEntity.getPartyId()));
	}

	public static SmsBatchRequest toSmsBatchRequest() {
		return new SmsBatchRequest();
	}

	public static DigitalMailRequest toDigitalMailRequest(final MessageEntity messageEntity, final String partyId) {
		return Optional.ofNullable(messageEntity).map(_ -> new DigitalMailRequest()
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
		return Optional.ofNullable(attachmentEntity).map(_ -> new DigitalMailAttachment()
			.filename(attachmentEntity.getFileName())
			.content(attachmentEntity.getContentString())
			.contentType(DigitalMailAttachment.ContentTypeEnum.fromValue(attachmentEntity.getContentType())))
			.orElse(null);
	}

	public static SnailmailRequest toSnailmailRequest(final MessageEntity messageEntity, final RecipientEntity recipientEntity) {
		if (anyNull(messageEntity, recipientEntity)) {
			return null;
		}
		return new SnailmailRequest()
			.party(new SnailmailParty().partyId(recipientEntity.getPartyId()))
			.address(toAddress(recipientEntity))
			.attachments(toSnailmailAttachments(messageEntity.getAttachments()))
			.department(messageEntity.getDepartment().getName())
			.folderName(messageEntity.getDepartment().getFolderName());
	}

	public static List<SnailmailAttachment> toSnailmailAttachments(final List<AttachmentEntity> attachmentEntities) {
		return Optional.ofNullable(attachmentEntities).orElse(emptyList()).stream()
			.map(MessagingMapper::toSnailmailAttachment)
			.filter(Objects::nonNull)
			.toList();
	}

	public static SnailmailAttachment toSnailmailAttachment(final AttachmentEntity attachmentEntity) {
		return Optional.ofNullable(attachmentEntity).map(_ -> new SnailmailAttachment()
			.filename(attachmentEntity.getFileName())
			.content(attachmentEntity.getContentString())
			.contentType(attachmentEntity.getContentType()))
			.orElse(null);
	}

	public static Address toAddress(final RecipientEntity recipientEntity) {
		return Optional.ofNullable(recipientEntity).map(_ -> new Address()
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

	public static List<EmailAttachment> toEmailAttachments(final MessageEntity messageEntity) {
		final var emailAttachments = new ArrayList<EmailAttachment>();
		ofNullable(messageEntity.getAttachments()).orElse(emptyList())
			.forEach(entity -> emailAttachments.add(toEmailAttachment(entity)));

		return emailAttachments;
	}

	public static EmailRequest toEmailRequest(final RecipientEntity recipientEntity, final Map<String, String> settingsMap) {
		// Extract email and subject from messagingSettings
		final var emailAddress = settingsMap.get(SNAILMAIL_CALLBACK_EMAIL);
		final var emailSubject = settingsMap.get(SNAILMAIL_CALLBACK_SUBJECT);

		return new EmailRequest()
			.party(new EmailRequestParty().partyId(UUID.fromString(recipientEntity.getPartyId())))
			.emailAddress(emailAddress)
			.subject(emailSubject)
			.sender(new EmailSender()
				.address(EMAIL_SENDER_ADDRESS)
				.name(EMAIL_SENDER_NAME))
			.message(MAIL_CONTENT.formatted(
				recipientEntity.getFirstName(), recipientEntity.getLastName(),
				recipientEntity.getStreetAddress(),
				recipientEntity.getCareOf(),
				recipientEntity.getZipCode(),
				recipientEntity.getCity()));
	}

	public static EmailAttachment toEmailAttachment(final AttachmentEntity attachmentEntity) {
		// Create attachments to be included in the email
		return new EmailAttachment()
			.contentType(attachmentEntity.getContentType())
			.name(attachmentEntity.getFileName())
			.content(blobToBase64(attachmentEntity.getContent()));
	}

	private static String blobToBase64(final Blob blob) {
		if (blob == null) {
			throw Problem.valueOf(BAD_GATEWAY, "No attachment, nothing to attach in email.");
		}

		try {
			byte[] bytes = blob.getBytes(1, (int) blob.length());
			return Base64.getEncoder().encodeToString(bytes);
		} catch (SQLException e) {
			throw Problem.valueOf(BAD_GATEWAY, "Couldn't read blob from entity, not sending email. " + e.getMessage());
		}
	}

}
