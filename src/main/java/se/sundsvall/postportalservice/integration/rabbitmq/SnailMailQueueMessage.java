package se.sundsvall.postportalservice.integration.rabbitmq;

import java.util.List;

/**
 * Payload published to the messaging service on {@code api-fabriken.messaging} with routing key {@code snail-mail}.
 * <p>
 * {@code batchId} is this service's message id, which is what it has always sent as the batch identity over REST. It
 * groups the recipients of one letter for snailmail-sender, which flushes the batch on its own schedule - so it is a
 * grouping key, not a coordination point, and nothing here waits for a batch to be complete.
 * <p>
 * Attachments are named by the object ids they were uploaded under rather than carried inline. That matters more here
 * than on any other channel: the attachments <em>are</em> the letter, so inlining them would put the whole document on
 * the queue, replicated across three nodes and kept through every retry tier.
 */
public record SnailMailQueueMessage(
	String municipalityId,
	String messageId,
	String recipientId,
	String batchId,
	String partyId,
	String department,
	String folderName,
	String deviation,
	Address address,
	List<Attachment> attachments,
	String sentBy,
	String origin)
	implements
	QueueMessage {

	public record Address(
		String firstName,
		String lastName,
		String organizationName,
		String address,
		String apartmentNumber,
		String careOf,
		String zipCode,
		String city,
		String country) {
	}

	public record Attachment(String filename, String contentType, String objectId) {
	}
}
