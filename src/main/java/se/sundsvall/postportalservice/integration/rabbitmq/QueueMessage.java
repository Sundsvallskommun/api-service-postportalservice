package se.sundsvall.postportalservice.integration.rabbitmq;

/**
 * What every payload published towards the messaging service has in common.
 * <p>
 * {@code recipientId} is this service's own id for one recipient's delivery. It is the correlation key the outcome
 * comes back under, and the idempotency key messaging is expected to pass on to the sender. It has to originate here:
 * messaging's work queue dead-letters through a retry ladder, so the same message can be consumed more than once, and a
 * key minted at consume time would differ between those attempts and dedupe nothing.
 */
public interface QueueMessage {

	String recipientId();
}
