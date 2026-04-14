package se.sundsvall.postportalservice.integration.rabbitmq.model;

public final class Constants {

	private Constants() {}

	public static final String POST_PORTAL_SERVICE_EXCHANGE = "postportalservice.exchange";
	public static final String SEND_DIGITAL_REGISTERED_LETTER_QUEUE = "task.send.digitalregisteredletter";

	public static final String DIGITAL_REGISTERED_LETTER_EXCHANGE = "digitalregisteredletter.exchange";
	public static final String STATUS_DIGITAL_REGISTERED_LETTER_QUEUE = "event.status.digitalregisteredletter";
}
