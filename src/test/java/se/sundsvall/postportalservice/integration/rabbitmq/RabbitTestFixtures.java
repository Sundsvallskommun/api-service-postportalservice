package se.sundsvall.postportalservice.integration.rabbitmq;

import java.util.List;
import se.sundsvall.postportalservice.integration.rabbitmq.RabbitIntegrationConfiguration.RabbitIntegrationProperties;

final class RabbitTestFixtures {

	static final String EXCHANGE = "api-fabriken.messaging";
	static final String RECIPIENT_ID = "8a2a0c66-8a4a-4a8b-9a91-b3b0e8dbb0f9";
	static final String MESSAGE_ID = "1a2b3c";
	static final String PARTY_ID = "6d0773d6-3e7f-4552-81bc-f0007af95adf";
	static final String OBJECT_ID = "f8e2bd3c-1a6b-4f5e-9d0a-2c7b1e4f6a58";

	private RabbitTestFixtures() {}

	/**
	 * The routing keys and status queues as they are committed in application.yml, so a test that asserts on one is
	 * asserting on what the service actually publishes to.
	 */
	static RabbitIntegrationProperties properties() {
		return new RabbitIntegrationProperties(true, EXCHANGE, 1,
			channel("sms"), channel("email"), channel("digital-mail"), channel("snail-mail"));
	}

	private static RabbitIntegrationProperties.Channel channel(final String key) {
		return new RabbitIntegrationProperties.Channel(key, "api-fabriken.postportal." + key + "-status");
	}

	static DigitalMailQueueMessage digitalMailQueueMessage() {
		return new DigitalMailQueueMessage(
			"2281", MESSAGE_ID, RECIPIENT_ID, PARTY_ID, "162021005489",
			"Subject", "Hello", "text/plain", "Kommunstyrelsekontoret",
			"Support text", "support@sundsvall.se", "+46701740605", "https://sundsvall.se/support",
			List.of(new DigitalMailQueueMessage.Attachment("file.pdf", "application/pdf", OBJECT_ID)),
			"joe01doe; type=adAccount", "PostPortalService");
	}

	static SnailMailQueueMessage snailMailQueueMessage() {
		return new SnailMailQueueMessage(
			"2281", MESSAGE_ID, RECIPIENT_ID, MESSAGE_ID, PARTY_ID,
			"Kommunstyrelsekontoret", "Sundsvalls Kommun", null,
			new SnailMailQueueMessage.Address("John", "Doe", "Acme AB", "Main Street 1", "1101", "c/o Jane Doe", "12345", "Sundsvall", "Sweden"),
			List.of(new SnailMailQueueMessage.Attachment("file.pdf", "application/pdf", OBJECT_ID)),
			"joe01doe; type=adAccount", "PostPortalService");
	}
}
