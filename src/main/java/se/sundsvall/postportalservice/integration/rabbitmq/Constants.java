package se.sundsvall.postportalservice.integration.rabbitmq;

public final class Constants {

	private Constants() {}

	public static final String DIGITAL_REGISTERED_LETTER_EXCHANGE = "digital-registered-letter";
	public static final String SEND_REGISTERED_LETTER_QUEUE = "send";
	public static final String STATUS_REGISTERED_LETTER_QUEUE = "status";
}
