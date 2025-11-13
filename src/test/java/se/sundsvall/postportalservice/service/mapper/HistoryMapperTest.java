package se.sundsvall.postportalservice.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.postportalservice.integration.db.converter.MessageType.DIGITAL_MAIL;

import generated.se.sundsvall.digitalregisteredletter.LetterStatus;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.postportalservice.integration.db.AttachmentEntity;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;

class HistoryMapperTest {

	private static final HistoryMapper HISTORY_MAPPER = new HistoryMapper();

	@Test
	void toMessageList() {
		// Setup
		final var created = OffsetDateTime.now();
		final var id = "id";
		final var messageType = DIGITAL_MAIL;
		final var recipients = List.of(RecipientEntity.create(), RecipientEntity.create(), RecipientEntity.create());
		final var subject = "subject";

		final var messageEntity = MessageEntity.create()
			.withCreated(created)
			.withId(id)
			.withMessageType(messageType)
			.withRecipients(recipients)
			.withSubject(subject);

		// Act
		final var result = HISTORY_MAPPER.toMessageList(List.of(messageEntity));

		// Assert
		assertThat(result).hasSize(1).satisfiesExactly(message -> {
			assertThat(message).hasNoNullFieldsOrPropertiesExcept("signingStatus");
			assertThat(message.getMessageId()).isEqualTo(id);
			assertThat(message.getNumberOfRecipients()).isEqualTo(recipients.size());
			assertThat(message.getSentAt()).isEqualTo(created.toLocalDateTime());
			assertThat(message.getSubject()).isEqualTo(subject);
			assertThat(message.getType()).isEqualTo(messageType.name());
		});
	}

	@Test
	void toMessageListWhenSourceContainsNull() {
		// Setup
		final var created = OffsetDateTime.now();
		final var id = "id";
		final var messageType = DIGITAL_MAIL;
		final var recipients = List.of(RecipientEntity.create());
		final var subject = "subject";

		final var messageEntity = MessageEntity.create()
			.withCreated(created)
			.withId(id)
			.withMessageType(messageType)
			.withSubject(subject)
			.withRecipients(recipients);

		final var entities = new ArrayList<>(List.of(messageEntity));
		entities.addFirst(null);

		// Act
		final var result = HISTORY_MAPPER.toMessageList(entities);

		// Assert
		assertThat(result).hasSize(1).satisfiesExactly(message -> {
			assertThat(message).hasNoNullFieldsOrPropertiesExcept("signingStatus");
			assertThat(message.getMessageId()).isEqualTo(id);
			assertThat(message.getNumberOfRecipients()).isEqualTo(recipients.size());
			assertThat(message.getSentAt()).isEqualTo(created.toLocalDateTime());
			assertThat(message.getSubject()).isEqualTo(subject);
			assertThat(message.getType()).isEqualTo(messageType.name());
		});
	}

	@Test
	void toMessageListFromNull() {
		assertThat(HISTORY_MAPPER.toMessageList(null)).isEmpty();
	}

	@Test
	void toMessage() {
		// Setup
		final var created = OffsetDateTime.now();
		final var id = "id";
		final var messageType = DIGITAL_MAIL;
		final var recipients = List.of(RecipientEntity.create(), RecipientEntity.create());
		final var subject = "subject";

		final var messageEntity = MessageEntity.create()
			.withCreated(created)
			.withId(id)
			.withMessageType(messageType)
			.withRecipients(recipients)
			.withSubject(subject);

		// Act
		final var result = HISTORY_MAPPER.toMessage(messageEntity);

		// Assert
		assertThat(result).isNotNull().hasNoNullFieldsOrPropertiesExcept("signingStatus");
		assertThat(result.getMessageId()).isEqualTo(id);
		assertThat(result.getNumberOfRecipients()).isEqualTo(recipients.size());
		assertThat(result.getSentAt()).isEqualTo(created.toLocalDateTime());
		assertThat(result.getSubject()).isEqualTo(subject);
		assertThat(result.getType()).isEqualTo(messageType.name());
	}

	@Test
	void toMessageForEmptySource() {
		// Setup
		final var messageEntity = MessageEntity.create();

		// Act
		final var result = HISTORY_MAPPER.toMessage(messageEntity);

		// Assert
		assertThat(result).isNotNull()
			.hasAllNullFieldsOrPropertiesExcept("numberOfRecipients")
			.extracting("numberOfRecipients").isEqualTo(0);
	}

	@Test
	void toMessageFromNull() {
		assertThat(HISTORY_MAPPER.toMessage(null)).isNull();
	}

	@Test
	void toMessageDetails() {
		// Setup
		final var subject = "subject";
		final var body = "body";
		final var created = OffsetDateTime.now();
		final var attachment = AttachmentEntity.create();
		final var recipient = RecipientEntity.create();
		final var messageEntity = MessageEntity.create()
			.withSubject(subject)
			.withBody(body)
			.withCreated(created)
			.withAttachments(List.of(attachment))
			.withRecipients(List.of(recipient));

		// Act
		final var result = HISTORY_MAPPER.toMessageDetails(messageEntity);

		// Assert
		assertThat(result).hasNoNullFieldsOrPropertiesExcept("signingStatus");
		assertThat(result.getSentAt()).isEqualTo(created.toLocalDateTime());
		assertThat(result.getSubject()).isEqualTo(subject);
		assertThat(result.getBody()).isEqualTo(body);
		assertThat(result.getAttachments()).hasSize(1);
		assertThat(result.getRecipients()).hasSize(1);
		assertThat(result.getSigningStatus()).isNull(); // Signing status is set by the service layer, not mapper
	}

	@Test
	void toMessageDetailsForEmptySource() {
		// Setup
		final var messageEntity = MessageEntity.create();

		// Act
		final var result = HISTORY_MAPPER.toMessageDetails(messageEntity);

		// Assert
		assertThat(result).isNotNull().hasAllNullFieldsOrPropertiesExcept("attachments", "recipients");
		assertThat(result.getAttachments()).isEmpty();
		assertThat(result.getRecipients()).isEmpty();
	}

	@Test
	void toMessageDetailsFromNull() {
		assertThat(HISTORY_MAPPER.toMessageDetails(null)).isNull();
	}

	@Test
	void toAttachmentList() {
		// Setup
		final var contentType = "contentType";
		final var fileName = "fileName";
		final var id = "id";
		final var attachmentEntity = AttachmentEntity.create()
			.withContentType(contentType)
			.withFileName(fileName)
			.withId(id);

		// Act
		final var result = HISTORY_MAPPER.toAttachmentList(List.of(attachmentEntity));

		// Assert
		assertThat(result).hasSize(1).satisfiesExactly(attachmentDetails -> {
			assertThat(attachmentDetails.getAttachmentId()).isEqualTo(id);
			assertThat(attachmentDetails.getContentType()).isEqualTo(contentType);
			assertThat(attachmentDetails.getFileName()).isEqualTo(fileName);
		});
	}

	@Test
	void toAttachmentListWhenSourceContainsNull() {
		// Setup
		final var contentType = "contentType";
		final var fileName = "fileName";
		final var id = "id";
		final var attachmentEntity = AttachmentEntity.create()
			.withContentType(contentType)
			.withFileName(fileName)
			.withId(id);

		final var entities = new ArrayList<>(List.of(attachmentEntity));
		entities.addFirst(null);

		// Act
		final var result = HISTORY_MAPPER.toAttachmentList(entities);

		// Assert
		assertThat(result).hasSize(1).satisfiesExactly(attachmentDetails -> {
			assertThat(attachmentDetails.getAttachmentId()).isEqualTo(id);
			assertThat(attachmentDetails.getContentType()).isEqualTo(contentType);
			assertThat(attachmentDetails.getFileName()).isEqualTo(fileName);
		});
	}

	@Test
	void toAttachmentListFromNull() {
		assertThat(HISTORY_MAPPER.toAttachmentList(null)).isEmpty();
	}

	@Test
	void toAttachment() {
		// Setup
		final var contentType = "contentType";
		final var fileName = "fileName";
		final var id = "id";
		final var attachmentEntity = AttachmentEntity.create()
			.withContentType(contentType)
			.withFileName(fileName)
			.withId(id);

		// Act
		final var result = HISTORY_MAPPER.toAttachment(attachmentEntity);

		assertThat(result).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(result.getAttachmentId()).isEqualTo(id);
		assertThat(result.getContentType()).isEqualTo(contentType);
		assertThat(result.getFileName()).isEqualTo(fileName);
	}

	@Test
	void toAttachmentFromEmptySource() {
		// Setup
		final var attachmentEntity = AttachmentEntity.create();

		// Act
		final var result = HISTORY_MAPPER.toAttachment(attachmentEntity);

		assertThat(result).isNotNull().hasAllNullFieldsOrProperties();
	}

	@Test
	void toAttachmentFromNull() {
		assertThat(HISTORY_MAPPER.toAttachment(null)).isNull();
	}

	@Test
	void toRecipientList() {
		// Setup
		final var city = "city";
		final var firstName = "firstName";
		final var lastName = "lastName";
		final var messageType = DIGITAL_MAIL;
		final var phoneNumber = "phoneNumber";
		final var partyId = "partyId";
		final var status = "status";
		final var streetAddress = "streetAddress";
		final var zipCode = "zipCode";
		final var recipientEntity = RecipientEntity.create()
			.withCity(city)
			.withFirstName(firstName)
			.withLastName(lastName)
			.withMessageType(messageType)
			.withPhoneNumber(phoneNumber)
			.withPartyId(partyId)
			.withStatus(status)
			.withStreetAddress(streetAddress)
			.withZipCode(zipCode);

		// Act
		final var result = HISTORY_MAPPER.toRecipientList(List.of(recipientEntity));

		// Assert
		assertThat(result).hasSize(1).satisfiesExactly(recipient -> {
			assertThat(recipient.getCity()).isEqualTo(city);
			assertThat(recipient.getMessageType()).isEqualTo(messageType.name());
			assertThat(recipient.getMobileNumber()).isEqualTo(phoneNumber);
			assertThat(recipient.getName()).isEqualTo(firstName + " " + lastName);
			assertThat(recipient.getPartyId()).isEqualTo(partyId);
			assertThat(recipient.getStatus()).isEqualTo(status);
			assertThat(recipient.getStreetAddress()).isEqualTo(streetAddress);
			assertThat(recipient.getZipCode()).isEqualTo(zipCode);
		});
	}

	@Test
	void toRecipientListWhenSourceContainsNull() {
		// Setup
		final var city = "city";
		final var firstName = "firstName";
		final var lastName = "lastName";
		final var messageType = DIGITAL_MAIL;
		final var phoneNumber = "phoneNumber";
		final var partyId = "partyId";
		final var status = "status";
		final var streetAddress = "streetAddress";
		final var zipCode = "zipCode";
		final var recipientEntity = RecipientEntity.create()
			.withCity(city)
			.withFirstName(firstName)
			.withLastName(lastName)
			.withMessageType(messageType)
			.withPhoneNumber(phoneNumber)
			.withPartyId(partyId)
			.withStatus(status)
			.withStreetAddress(streetAddress)
			.withZipCode(zipCode);
		final var entities = new ArrayList<>(List.of(recipientEntity));
		entities.addFirst(null);

		// Act
		final var result = HISTORY_MAPPER.toRecipientList(entities);

		// Assert
		assertThat(result).hasSize(1).satisfiesExactly(recipient -> {
			assertThat(recipient.getCity()).isEqualTo(city);
			assertThat(recipient.getMessageType()).isEqualTo(messageType.name());
			assertThat(recipient.getMobileNumber()).isEqualTo(phoneNumber);
			assertThat(recipient.getName()).isEqualTo(firstName + " " + lastName);
			assertThat(recipient.getPartyId()).isEqualTo(partyId);
			assertThat(recipient.getStatus()).isEqualTo(status);
			assertThat(recipient.getStreetAddress()).isEqualTo(streetAddress);
			assertThat(recipient.getZipCode()).isEqualTo(zipCode);
		});
	}

	@Test
	void toRecipientListFromNull() {
		assertThat(HISTORY_MAPPER.toRecipientList(null)).isEmpty();
	}

	@Test
	void toRecipient() {
		// Setup
		final var city = "city";
		final var firstName = "firstName";
		final var lastName = "lastName";
		final var messageType = DIGITAL_MAIL;
		final var phoneNumber = "phoneNumber";
		final var partyId = "partyId";
		final var status = "status";
		final var streetAddress = "streetAddress";
		final var zipCode = "zipCode";
		final var recipientEntity = RecipientEntity.create()
			.withCity(city)
			.withFirstName(firstName)
			.withLastName(lastName)
			.withMessageType(messageType)
			.withPhoneNumber(phoneNumber)
			.withPartyId(partyId)
			.withStatus(status)
			.withStreetAddress(streetAddress)
			.withZipCode(zipCode);

		// Act
		final var result = HISTORY_MAPPER.toRecipient(recipientEntity);

		// Assert
		assertThat(result).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(result.getCity()).isEqualTo(city);
		assertThat(result.getMessageType()).isEqualTo(messageType.name());
		assertThat(result.getMobileNumber()).isEqualTo(phoneNumber);
		assertThat(result.getName()).isEqualTo("%s %s".formatted(firstName, lastName));
		assertThat(result.getPartyId()).isEqualTo(partyId);
		assertThat(result.getStatus()).isEqualTo(status);
		assertThat(result.getStreetAddress()).isEqualTo(streetAddress);
		assertThat(result.getZipCode()).isEqualTo(zipCode);
	}

	@Test
	void toRecipientFromEmptySource() {
		// Setup
		final var recipientEntity = RecipientEntity.create();

		// Act
		final var result = HISTORY_MAPPER.toRecipient(recipientEntity);

		// Assert
		assertThat(result).isNotNull().hasAllNullFieldsOrProperties();
	}

	@Test
	void toRecipientFromNull() {
		assertThat(HISTORY_MAPPER.toRecipient(null)).isNull();
	}

	@Test
	void toSigningStatus() {
		// Setup
		final var status = "status";
		final var signingInformation = "signingInformation";
		final var letterStatus = new LetterStatus()
			.status(status)
			.signingInformation(signingInformation);

		// Act
		final var result = HISTORY_MAPPER.toSigningStatus(letterStatus);

		assertThat(result).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(result.getLetterState()).isEqualTo(status);
		assertThat(result.getSigningProcessState()).isEqualTo(signingInformation);
	}

	@Test
	void toSigningStatusFromEmptySource() {
		// Setup
		final var letterStatus = new LetterStatus();

		// Act
		final var result = HISTORY_MAPPER.toSigningStatus(letterStatus);

		assertThat(result).isNotNull().hasAllNullFieldsOrProperties();
	}

	@Test
	void toSigningStatusFromNull() {
		assertThat(HISTORY_MAPPER.toSigningStatus(null)).isNull();
	}
}
