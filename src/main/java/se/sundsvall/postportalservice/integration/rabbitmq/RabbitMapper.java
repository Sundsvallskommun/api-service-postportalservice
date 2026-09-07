package se.sundsvall.postportalservice.integration.rabbitmq;

import generated.se.sundsvall.messaging.EmailSender;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.messaging.MessagingMapper;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.ObjectUtils.anyNull;
import static se.sundsvall.postportalservice.Constants.ORIGIN;
import static se.sundsvall.postportalservice.service.util.IdentifierUtil.getIdentifierHeaderValue;

public final class RabbitMapper {

	private RabbitMapper() {}

	public static SmsQueueMessage toSmsQueueMessage(final MessageEntity messageEntity, final RecipientEntity recipientEntity) {
		if (anyNull(messageEntity, recipientEntity)) {
			return null;
		}
		return new SmsQueueMessage(
			messageEntity.getMunicipalityId(),
			messageEntity.getId(),
			recipientEntity.getId(),
			recipientEntity.getPartyId(),
			recipientEntity.getPhoneNumber(),
			messageEntity.getDisplayName(),
			messageEntity.getDepartment().getName(),
			messageEntity.getBody(),
			getIdentifierHeaderValue(messageEntity.getUser().getUsername()),
			ORIGIN);
	}

	/**
	 * Builds the e-mail queue message.
	 * <p>
	 * The addressing, subject, sender and body all come from {@link MessagingMapper#toEmailRequest}, the same builder
	 * the REST path uses, so that switching a deployment between the two cannot change what the recipient reads.
	 * Attachments are the one deliberate difference: they are named by the object ids they were uploaded under rather
	 * than carried inline. The caller does the uploading, since a failure to store is a failure to send and belongs
	 * where the recipient can be marked for it.
	 *
	 * @param objectIds the stored object id for each of the message's attachments, in the same order
	 */
	public static EmailQueueMessage toEmailQueueMessage(final MessageEntity messageEntity, final RecipientEntity recipientEntity,
		final Map<String, String> settingsMap, final List<String> objectIds) {

		if (anyNull(messageEntity, recipientEntity, settingsMap, objectIds)) {
			return null;
		}

		final var emailRequest = MessagingMapper.toEmailRequest(recipientEntity, settingsMap);

		return new EmailQueueMessage(
			messageEntity.getMunicipalityId(),
			messageEntity.getId(),
			recipientEntity.getId(),
			recipientEntity.getPartyId(),
			emailRequest.getEmailAddress(),
			emailRequest.getSubject(),
			emailRequest.getMessage(),
			emailRequest.getHtmlMessage(),
			ofNullable(emailRequest.getSender()).map(EmailSender::getName).orElse(null),
			ofNullable(emailRequest.getSender()).map(EmailSender::getAddress).orElse(null),
			ofNullable(emailRequest.getSender()).map(EmailSender::getReplyTo).orElse(null),
			toAttachments(messageEntity, objectIds),
			getIdentifierHeaderValue(messageEntity.getUser().getUsername()),
			ORIGIN);
	}

	private static List<EmailQueueMessage.Attachment> toAttachments(final MessageEntity messageEntity, final List<String> objectIds) {
		final var attachments = ofNullable(messageEntity.getAttachments()).orElse(List.of());
		final var result = new ArrayList<EmailQueueMessage.Attachment>();

		for (var index = 0; index < attachments.size(); index++) {
			final var attachment = attachments.get(index);
			result.add(new EmailQueueMessage.Attachment(
				attachment.getFileName(),
				attachment.getContentType(),
				objectIds.get(index)));
		}

		return result;
	}
}
