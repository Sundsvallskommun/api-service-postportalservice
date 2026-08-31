package se.sundsvall.postportalservice.integration.rabbitmq;

/**
 * Payload published to the messaging service on {@code api-fabriken.messaging} with routing key {@code sms}.
 * <p>
 * {@code recipientId} is both the correlation key for the status message that comes back on
 * {@code api-fabriken.postportal.sms-status} and the idempotency key messaging is expected to pass on to sms-sender.
 * It has to originate here: messaging's work queue dead-letters through a retry ladder, so the same message can be
 * consumed more than once, and a key minted at consume time would differ between those attempts and dedupe nothing.
 * <p>
 * {@code sentBy} and {@code origin} carry what the {@code X-Sent-By} and {@code x-origin} headers carried on the HTTP
 * call this replaces. {@code sentBy} keeps the full identifier syntax rather than the bare value, because the type is
 * part of the meaning - the same value can be an AD account or a party id - and the consumer is what decides how much
 * of it to keep. AMQP headers on this path are reserved for the retry ladder's own mechanics.
 */
public record SmsQueueMessage(
	String municipalityId,
	String messageId,
	String recipientId,
	String partyId,
	String mobileNumber,
	String sender,
	String department,
	String message,
	String sentBy,
	String origin) {
}
