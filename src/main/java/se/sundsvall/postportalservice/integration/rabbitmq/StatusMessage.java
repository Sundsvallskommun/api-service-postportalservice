package se.sundsvall.postportalservice.integration.rabbitmq;

/**
 * The single terminal outcome the messaging service publishes per send request, whatever channel it was sent on.
 * <p>
 * One record per channel rather than one shared record, because each is the wire contract for its own queue and its
 * name is what messaging stamps into the {@code __TypeId__} header. The interface is what lets the resolution and the
 * database write be written once.
 * <p>
 * {@code externalId} is messaging's own message id, which is what lets a delivery be traced back into its history.
 */
public interface StatusMessage {

	String recipientId();

	String status();

	String externalId();

	String statusDetail();
}
