package se.sundsvall.postportalservice.integration.rabbitmq;

/**
 * The single terminal outcome the messaging service publishes per digital mail send request, consumed from
 * {@code api-fabriken.postportal.digital-mail-status}.
 * <p>
 * {@code externalId} is messaging's own message id, which is what lets a delivery be traced back into its history.
 */
public record DigitalMailStatusMessage(
	String recipientId,
	String status,
	String externalId,
	String statusDetail)
	implements
	StatusMessage {
}
