package se.sundsvall.postportalservice.integration.objectstore.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("integration.object-store")
public record ObjectStoreProperties(
	@DefaultValue("5") int connectTimeout,
	@DefaultValue("30") int readTimeout,
	/**
	 * The bucket attachments are uploaded to. It is configuration rather than something a caller names, and messaging
	 * reads from the same one - the reference on the queue carries only an object id, so the two must agree here.
	 */
	String bucket,
	/**
	 * How long an uploaded object is kept. It has to outlive the longest journey the reference can take: the retry
	 * ladder, the time a message can sit in the parking lot, and any manual replay from it. Too short and a replayed
	 * message points at nothing.
	 */
	@DefaultValue("P7D") String timeToLive) {
}
