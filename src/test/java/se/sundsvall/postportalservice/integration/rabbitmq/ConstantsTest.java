package se.sundsvall.postportalservice.integration.rabbitmq;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.postportalservice.integration.rabbitmq.Constants.DIGITAL_REGISTERED_LETTER_EXCHANGE;
import static se.sundsvall.postportalservice.integration.rabbitmq.Constants.SEND_REGISTERED_LETTER_QUEUE;
import static se.sundsvall.postportalservice.integration.rabbitmq.Constants.STATUS_REGISTERED_LETTER_QUEUE;

class ConstantsTest {

	@Test
	void constants() {
		assertThat(DIGITAL_REGISTERED_LETTER_EXCHANGE).isEqualTo("digital-registered-letter");
		assertThat(SEND_REGISTERED_LETTER_QUEUE).isEqualTo("send");
		assertThat(STATUS_REGISTERED_LETTER_QUEUE).isEqualTo("status");
	}
}
