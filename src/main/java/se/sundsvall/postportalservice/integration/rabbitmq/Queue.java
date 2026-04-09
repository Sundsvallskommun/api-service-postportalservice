package se.sundsvall.postportalservice.integration.rabbitmq;

import static se.sundsvall.postportalservice.integration.rabbitmq.Constants.DIGITAL_REGISTERED_LETTER_EXCHANGE;
import static se.sundsvall.postportalservice.integration.rabbitmq.Constants.SEND_REGISTERED_LETTER_QUEUE;
import static se.sundsvall.postportalservice.integration.rabbitmq.Constants.STATUS_REGISTERED_LETTER_QUEUE;

public enum Queue {

	SEND_REGISTERED_LETTER(DIGITAL_REGISTERED_LETTER_EXCHANGE, SEND_REGISTERED_LETTER_QUEUE),
	STATUS_REGISTERED_LETTER(DIGITAL_REGISTERED_LETTER_EXCHANGE, STATUS_REGISTERED_LETTER_QUEUE);

	private final String exchange;
	private final String routingKey;

	Queue(final String exchange, final String routingKey) {
		this.exchange = exchange;
		this.routingKey = routingKey;
	}

	public String getExchange() {
		return exchange;
	}

	public String getRoutingKey() {
		return routingKey;
	}

}
