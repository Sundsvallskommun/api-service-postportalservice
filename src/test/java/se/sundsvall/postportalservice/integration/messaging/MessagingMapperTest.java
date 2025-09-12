package se.sundsvall.postportalservice.integration.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import generated.se.sundsvall.messaging.DigitalMailRequest;
import generated.se.sundsvall.messaging.SmsBatchRequest;
import generated.se.sundsvall.messaging.SmsRequest;
import org.junit.jupiter.api.Test;
import se.sundsvall.postportalservice.integration.db.DepartmentEntity;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;

class MessagingMapperTest {

	@Test
	void toSmsRequest() {
		var departmentEntity = DepartmentEntity.create()
			.withName("DepartmentName");
		var messageEntity = MessageEntity.create()
			.withText("Text")
			.withDepartment(departmentEntity);
		var recipientEntity = RecipientEntity.create()
			.withPhoneNumber("123456789")
			.withPartyId("PartyId");

		var result = MessagingMapper.toSmsRequest(messageEntity, recipientEntity);
		assertThat(result).isInstanceOf(SmsRequest.class);
	}

	@Test
	void toSmsBatchRequest() {
		var result = MessagingMapper.toSmsBatchRequest();
		assertThat(result).isInstanceOf(SmsBatchRequest.class);
	}

	@Test
	void toDigitalMailRequest() {
		var result = MessagingMapper.toDigitalMailRequest();
		assertThat(result).isInstanceOf(DigitalMailRequest.class);
	}
}
