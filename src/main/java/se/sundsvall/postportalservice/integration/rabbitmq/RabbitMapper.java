package se.sundsvall.postportalservice.integration.rabbitmq;

import generated.se.sundsvall.messaging.EmailSender;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import se.sundsvall.postportalservice.integration.db.AttachmentEntity;
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
		return zip(messageEntity, objectIds,
			(attachment, objectId) -> new EmailQueueMessage.Attachment(attachment.getFileName(), attachment.getContentType(), objectId));
	}

	/**
	 * Builds the digital mail queue message.
	 * <p>
	 * The subject, body, department and support info all come from the same entity fields
	 * {@link MessagingMapper#toDigitalMailRequest} reads, so that switching a deployment between the two paths cannot
	 * change what the recipient reads. One party per message: the REST endpoint takes a list because one call may name
	 * many recipients, while a queued request is already one recipient's worth of work.
	 *
	 * @param objectIds the stored object id for each of the message's attachments, in the same order
	 */
	public static DigitalMailQueueMessage toDigitalMailQueueMessage(final MessageEntity messageEntity, final RecipientEntity recipientEntity,
		final List<String> objectIds) {

		if (anyNull(messageEntity, recipientEntity, objectIds)) {
			return null;
		}

		final var department = messageEntity.getDepartment();

		return new DigitalMailQueueMessage(
			messageEntity.getMunicipalityId(),
			messageEntity.getId(),
			recipientEntity.getId(),
			recipientEntity.getPartyId(),
			department.getOrganizationNumber(),
			messageEntity.getSubject(),
			messageEntity.getBody(),
			messageEntity.getContentType(),
			department.getName(),
			department.getSupportText(),
			department.getContactInformationEmail(),
			department.getContactInformationPhoneNumber(),
			department.getContactInformationUrl(),
			zip(messageEntity, objectIds,
				(attachment, objectId) -> new DigitalMailQueueMessage.Attachment(attachment.getFileName(), attachment.getContentType(), objectId)),
			getIdentifierHeaderValue(messageEntity.getUser().getUsername()),
			ORIGIN);
	}

	/**
	 * Builds the snail mail queue message.
	 * <p>
	 * The batch id is the message id, which is exactly what this service passes as {@code batchId} on the REST call it
	 * replaces: snailmail-sender groups a letter's recipients by it and posts the group as one job.
	 *
	 * @param objectIds the stored object id for each of the message's attachments, in the same order
	 */
	public static SnailMailQueueMessage toSnailMailQueueMessage(final MessageEntity messageEntity, final RecipientEntity recipientEntity,
		final List<String> objectIds) {

		if (anyNull(messageEntity, recipientEntity, objectIds)) {
			return null;
		}

		final var department = messageEntity.getDepartment();

		return new SnailMailQueueMessage(
			messageEntity.getMunicipalityId(),
			messageEntity.getId(),
			recipientEntity.getId(),
			messageEntity.getId(),
			recipientEntity.getPartyId(),
			department.getName(),
			department.getFolderName(),
			// Never set on this path today. Carried so the record stays in step with the one messaging consumes, which
			// has the field because the REST contract does.
			null,
			toAddress(recipientEntity),
			zip(messageEntity, objectIds,
				(attachment, objectId) -> new SnailMailQueueMessage.Attachment(attachment.getFileName(), attachment.getContentType(), objectId)),
			getIdentifierHeaderValue(messageEntity.getUser().getUsername()),
			ORIGIN);
	}

	private static SnailMailQueueMessage.Address toAddress(final RecipientEntity recipientEntity) {
		return new SnailMailQueueMessage.Address(
			recipientEntity.getFirstName(),
			recipientEntity.getLastName(),
			recipientEntity.getOrganizationName(),
			recipientEntity.getStreetAddress(),
			recipientEntity.getApartmentNumber(),
			recipientEntity.getCareOf(),
			recipientEntity.getZipCode(),
			recipientEntity.getCity(),
			recipientEntity.getCountry());
	}

	/**
	 * Pairs each attachment with the object id it was stored under. Positional rather than keyed, because the ids come
	 * straight back from the upload in the order the attachments were read - an index mismatch would silently send one
	 * recipient another attachment's bytes, so the two lists are only ever produced together.
	 */
	private static <A> List<A> zip(final MessageEntity messageEntity, final List<String> objectIds,
		final BiFunction<AttachmentEntity, String, A> combine) {

		final var attachments = ofNullable(messageEntity.getAttachments()).orElse(List.of());
		final var result = new ArrayList<A>();

		for (var index = 0; index < attachments.size(); index++) {
			result.add(combine.apply(attachments.get(index), objectIds.get(index)));
		}

		return result;
	}
}
