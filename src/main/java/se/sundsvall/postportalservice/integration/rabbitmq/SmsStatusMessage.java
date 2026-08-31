package se.sundsvall.postportalservice.integration.rabbitmq;

/**
 * Delivery outcome published by the messaging service onto {@code api-fabriken.messaging.status} with routing key
 * {@code sms.sent} or {@code sms.failed}, consumed from {@code api-fabriken.postportal.sms-status}, which is bound
 * {@code sms.*}.
 * <p>
 * {@code recipientId} is the value from the originating {@link SmsQueueMessage}. The outcome is carried by both the
 * routing key and {@code status}, and the payload wins where they disagree, since dead-lettering rewrites the routing
 * key. {@code externalId} is messaging's own message id, stored on the recipient so a delivery can still be traced back
 * into messaging.
 */
public record SmsStatusMessage(
	String recipientId,
	String status,
	String externalId,
	String statusDetail) {
}
