package se.sundsvall.postportalservice.integration.rabbitmq;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.postportalservice.integration.rabbitmq.model.Constants.DIGITAL_REGISTERED_LETTER_EXCHANGE;
import static se.sundsvall.postportalservice.integration.rabbitmq.model.Constants.POST_PORTAL_SERVICE_EXCHANGE;
import static se.sundsvall.postportalservice.integration.rabbitmq.model.Constants.SEND_DIGITAL_REGISTERED_LETTER_QUEUE;
import static se.sundsvall.postportalservice.integration.rabbitmq.model.Constants.STATUS_DIGITAL_REGISTERED_LETTER_QUEUE;

class ConstantsTest {

	@Test
	void constants() {
		assertThat(POST_PORTAL_SERVICE_EXCHANGE).isEqualTo("postportalservice.exchange");
		assertThat(DIGITAL_REGISTERED_LETTER_EXCHANGE).isEqualTo("digitalregisteredletter.exchange");
		assertThat(SEND_DIGITAL_REGISTERED_LETTER_QUEUE).isEqualTo("task.send.digitalregisteredletter");
		assertThat(STATUS_DIGITAL_REGISTERED_LETTER_QUEUE).isEqualTo("event.status.digitalregisteredletter");
	}
}
