package se.sundsvall.postportalservice.integration.rabbitmq;

/**
 * A channel's outcome once it has been reduced to one of the two terminal states.
 * <p>
 * Exists so that {@link StatusListener} can hand a corrected outcome onward without every channel's wire record needing
 * a copy constructor of its own. Never published anywhere - it only ever travels from the listener to the service that
 * writes it down.
 */
public record DeliveryOutcome(
	String recipientId,
	String status,
	String externalId,
	String statusDetail)
	implements
	StatusMessage {

	static DeliveryOutcome of(final StatusMessage statusMessage, final String status, final String statusDetail) {
		return new DeliveryOutcome(statusMessage.recipientId(), status, statusMessage.externalId(), statusDetail);
	}
}
