package se.sundsvall.postportalservice.integration.rabbitmq;

/**
 * The single terminal outcome the messaging service publishes per an e-mail send request, consumed from
 * {@code api-fabriken.postportal.email-status}.
 * <p>
 * {@code externalId} is messaging's own message id, which is what lets a delivery be traced back into its history.
 */
public record EmailStatusMessage(
	String recipientId,
	String status,
	String externalId,
	String statusDetail)
	implements
	StatusMessage {
}
