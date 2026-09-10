package se.sundsvall.postportalservice.integration.rabbitmq;

import java.util.List;

/**
 * Payload published to the messaging service on {@code api-fabriken.messaging} with routing key
 * {@code digital-mail}.
 * <p>
 * One party per message, deliberately. The REST endpoint takes a list because a caller may want one call for many
 * recipients; a queued request is already one recipient's worth of work, and keeping it that way is what lets each one
 * fail, retry and report on its own rather than dragging the others along.
 * <p>
 * Attachments are named by the object ids they were uploaded under rather than carried inline: a quorum queue is a poor
 * place to put megabytes, and those bytes would be replicated across three nodes, held in messaging's wait queues for
 * the length of its ladder, and kept in the parking lot after a give-up.
 * <p>
 * {@code sentBy} keeps the full identifier syntax rather than the bare value, because the type is part of the meaning -
 * the same value can be an AD account or a party id - and the consumer is what decides how much of it to keep.
 */
public record DigitalMailQueueMessage(
	String municipalityId,
	String messageId,
	String recipientId,
	String partyId,
	String organizationNumber,
	String subject,
	String body,
	String contentType,
	String department,
	String supportText,
	String supportEmailAddress,
	String supportPhoneNumber,
	String supportUrl,
	List<Attachment> attachments,
	String sentBy,
	String origin)
	implements
	QueueMessage {

	public record Attachment(String filename, String contentType, String objectId) {
	}
}
