package se.sundsvall.postportalservice.integration.rabbitmq;

/**
 * Delivery outcome published back by the messaging service on {@code api-fabriken.postportal} with routing key
 * {@code sms.status}, consumed from {@code api-fabriken.postportal.sms-status}.
 * <p>
 * {@code recipientId} is the value from the originating {@link SmsQueueMessage}. {@code externalId} is messaging's own
 * message id, stored on the recipient so a delivery can still be traced back into messaging.
 */
public record SmsStatusMessage(
	String recipientId,
	String status,
	String externalId,
	String statusDetail) {
}
