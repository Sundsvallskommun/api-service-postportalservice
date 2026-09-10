package se.sundsvall.postportalservice.integration.rabbitmq;

import java.util.List;

/**
 * A request to send an e-mail, published onto {@code api-fabriken.messaging} with routing key {@code email}.
 * <p>
 * This is the contract with the messaging service and must stay in step with the record of the same name over there.
 * {@code recipientId} is the correlation key: the outcome that comes back on the status queue carries it, and it is
 * what a duplicate outcome is recognised by.
 * <p>
 * Attachments travel as object references rather than as content. Sending the bytes would put them on a quorum queue -
 * replicated across three nodes, parked in a wait queue for the length of every retry, and kept in the parking lot
 * after a give-up. The reference costs the same whatever the file weighs.
 */
public record EmailQueueMessage(
	String municipalityId,
	String messageId,
	String recipientId,
	String partyId,
	String emailAddress,
	String subject,
	String message,
	String htmlMessage,
	String senderName,
	String senderAddress,
	String replyTo,
	List<Attachment> attachments,
	String sentBy,
	String origin)
	implements
	QueueMessage {

	public record Attachment(String name, String contentType, String objectId) {
	}
}
