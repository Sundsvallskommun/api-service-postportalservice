package se.sundsvall.postportalservice.integration.rabbitmq;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import se.sundsvall.postportalservice.integration.db.AttachmentEntity;
import se.sundsvall.postportalservice.integration.db.DepartmentEntity;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.db.UserEntity;
import se.sundsvall.postportalservice.integration.messaging.MessagingMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.postportalservice.integration.rabbitmq.RabbitMapper.toEmailQueueMessage;
import static se.sundsvall.postportalservice.integration.rabbitmq.RabbitMapper.toSmsQueueMessage;

class RabbitMapperTest {

	private static final String OBJECT_ID = "f8e2bd3c-1a6b-4f5e-9d0a-2c7b1e4f6a58";

	private static final Map<String, String> SETTINGS = Map.of(
		"callback_email", "callback@example.com",
		"callback_email_subject", "Subject");

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

	@Test
	void toEmailQueueMessage_takesItsAddressingAndBodyFromTheRestMapper() {
		final var messageEntity = messageEntityWithAttachment();
		final var recipientEntity = RecipientEntity.create()
			.withId("8a2a0c66-8a4a-4a8b-9a91-b3b0e8dbb0f9")
			.withPartyId("6d0773d6-3e7f-4552-81bc-f0007af95adf")
			.withStreetAddress("Gatan 1")
			.withZipCode("85230")
			.withCity("Sundsvall");

		final var result = toEmailQueueMessage(messageEntity, recipientEntity, SETTINGS, List.of(OBJECT_ID));

		// Everything but the attachments is built by the same mapper the REST path uses, so that switching a deployment
		// between the two cannot change what the recipient reads.
		final var restRequest = MessagingMapper.toEmailRequest(recipientEntity, SETTINGS);
		assertThat(result.emailAddress()).isEqualTo(restRequest.getEmailAddress());
		assertThat(result.subject()).isEqualTo(restRequest.getSubject());
		assertThat(result.message()).isEqualTo(restRequest.getMessage());
		assertThat(result.senderName()).isEqualTo(restRequest.getSender().getName());
		assertThat(result.senderAddress()).isEqualTo(restRequest.getSender().getAddress());

		assertThat(result.municipalityId()).isEqualTo("2281");
		assertThat(result.recipientId()).isEqualTo("8a2a0c66-8a4a-4a8b-9a91-b3b0e8dbb0f9");
		assertThat(result.sentBy()).isEqualTo("joe01doe; type=adAccount");
		assertThat(result.origin()).isEqualTo("PostPortalService");
	}

	@Test
	void toEmailQueueMessage_carriesAttachmentsByReferenceInOrder() {
		final var result = toEmailQueueMessage(messageEntityWithAttachment(), RecipientEntity.create()
			.withId("8a2a0c66-8a4a-4a8b-9a91-b3b0e8dbb0f9")
			.withPartyId("6d0773d6-3e7f-4552-81bc-f0007af95adf"), SETTINGS, List.of(OBJECT_ID));

		// The object ids are positional: the caller uploads in the order the attachments are held, and the reference
		// for each one has to line up with the file it names.
		assertThat(result.attachments()).singleElement().satisfies(attachment -> {
			assertThat(attachment.name()).isEqualTo("file.txt");
			assertThat(attachment.contentType()).isEqualTo("text/plain");
			assertThat(attachment.objectId()).isEqualTo(OBJECT_ID);
		});
	}

	@Test
	void toEmailQueueMessage_nullArguments() {
		final var recipientEntity = RecipientEntity.create();

		assertThat(toEmailQueueMessage(null, recipientEntity, SETTINGS, List.of())).isNull();
		assertThat(toEmailQueueMessage(MessageEntity.create(), null, SETTINGS, List.of())).isNull();
		assertThat(toEmailQueueMessage(MessageEntity.create(), recipientEntity, null, List.of())).isNull();
		assertThat(toEmailQueueMessage(MessageEntity.create(), recipientEntity, SETTINGS, null)).isNull();
	}

	private static MessageEntity messageEntityWithAttachment() {
		return MessageEntity.create()
			.withId("1a2b3c")
			.withMunicipalityId("2281")
			.withDisplayName("Sundsvall")
			.withBody("Hello")
			.withUser(UserEntity.create().withUsername("joe01doe"))
			.withDepartment(DepartmentEntity.create().withName("Department"))
			.withAttachments(List.of(AttachmentEntity.create()
				.withFileName("file.txt")
				.withContentType("text/plain")));
	}
}
