package se.sundsvall.postportalservice.integration.rabbitmq.model;

import static se.sundsvall.postportalservice.integration.rabbitmq.model.Constants.DIGITAL_REGISTERED_LETTER_EXCHANGE;
import static se.sundsvall.postportalservice.integration.rabbitmq.model.Constants.POST_PORTAL_SERVICE_EXCHANGE;
import static se.sundsvall.postportalservice.integration.rabbitmq.model.Constants.SEND_DIGITAL_REGISTERED_LETTER_QUEUE;
import static se.sundsvall.postportalservice.integration.rabbitmq.model.Constants.STATUS_DIGITAL_REGISTERED_LETTER_QUEUE;

public enum Queue {

	SEND_REGISTERED_LETTER(POST_PORTAL_SERVICE_EXCHANGE, SEND_DIGITAL_REGISTERED_LETTER_QUEUE),
	STATUS_REGISTERED_LETTER(DIGITAL_REGISTERED_LETTER_EXCHANGE, STATUS_DIGITAL_REGISTERED_LETTER_QUEUE);

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
