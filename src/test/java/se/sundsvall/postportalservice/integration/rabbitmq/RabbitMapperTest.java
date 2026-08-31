package se.sundsvall.postportalservice.integration.rabbitmq;

import org.junit.jupiter.api.Test;
import se.sundsvall.postportalservice.integration.db.DepartmentEntity;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.db.UserEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.postportalservice.integration.rabbitmq.RabbitMapper.toSmsQueueMessage;

class RabbitMapperTest {

	@Test
	void toSmsQueueMessage_mapsEveryField() {
		final var messageEntity = MessageEntity.create()
			.withId("1a2b3c")
			.withMunicipalityId("2281")
			.withDisplayName("Sundsvall")
			.withBody("Hello")
			.withUser(UserEntity.create().withUsername("joe01doe"))
			.withDepartment(DepartmentEntity.create().withName("Department"));
		final var recipientEntity = RecipientEntity.create()
			.withId("8a2a0c66-8a4a-4a8b-9a91-b3b0e8dbb0f9")
			.withPartyId("6d0773d6-3e7f-4552-81bc-f0007af95adf")
			.withPhoneNumber("+46701740605");

		final var result = toSmsQueueMessage(messageEntity, recipientEntity);

		assertThat(result.municipalityId()).isEqualTo("2281");
		assertThat(result.messageId()).isEqualTo("1a2b3c");
		assertThat(result.recipientId()).isEqualTo("8a2a0c66-8a4a-4a8b-9a91-b3b0e8dbb0f9");
		assertThat(result.partyId()).isEqualTo("6d0773d6-3e7f-4552-81bc-f0007af95adf");
		assertThat(result.mobileNumber()).isEqualTo("+46701740605");
		assertThat(result.sender()).isEqualTo("Sundsvall");
		assertThat(result.department()).isEqualTo("Department");
		assertThat(result.message()).isEqualTo("Hello");
		// The full identifier: the type travels with the value, since it can be an AD account or a party id.
		assertThat(result.sentBy()).isEqualTo("joe01doe; type=adAccount");
		assertThat(result.origin()).isEqualTo("PostPortalService");
		assertThat(result).hasNoNullFieldsOrProperties();
	}

	@Test
	void toSmsQueueMessage_nullMessage() {
		assertThat(toSmsQueueMessage(null, RecipientEntity.create())).isNull();
	}

	@Test
	void toSmsQueueMessage_nullRecipient() {
		assertThat(toSmsQueueMessage(MessageEntity.create(), null)).isNull();
	}
}
