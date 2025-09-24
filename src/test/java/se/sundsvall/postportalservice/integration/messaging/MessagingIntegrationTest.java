package se.sundsvall.postportalservice.integration.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.postportalservice.Constants.ORIGIN;
import static se.sundsvall.postportalservice.TestDataFactory.MUNICIPALITY_ID;

import generated.se.sundsvall.messaging.DeliveryResult;
import generated.se.sundsvall.messaging.DigitalMailAttachment;
import generated.se.sundsvall.messaging.DigitalMailRequest;
import generated.se.sundsvall.messaging.MessageBatchResult;
import generated.se.sundsvall.messaging.MessageResult;
import generated.se.sundsvall.messaging.MessageStatus;
import generated.se.sundsvall.messaging.SmsRequest;
import generated.se.sundsvall.messaging.SnailmailRequest;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.postportalservice.integration.db.AttachmentEntity;
import se.sundsvall.postportalservice.integration.db.DepartmentEntity;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.db.UserEntity;
import se.sundsvall.postportalservice.integration.db.converter.MessageType;

@ExtendWith(MockitoExtension.class)
class MessagingIntegrationTest {

	private static final String HEADER_VALUE = "John Wick; type=adAccount";

	@Mock
	private MessagingClient messagingClientMock;

	@Captor
	private ArgumentCaptor<SmsRequest> smsRequestCaptor;

	@Captor
	private ArgumentCaptor<DigitalMailRequest> digitalMailRequestArgumentCaptor;

	@Captor
	private ArgumentCaptor<SnailmailRequest> snailmailRequestCaptor;

	@InjectMocks
	private MessagingIntegration messagingIntegration;

	@AfterEach
	void verifyInteractions() {
		verifyNoMoreInteractions(messagingClientMock);
	}

	@Test
	void sendDigitalMail() throws SQLException {
		var blob = Mockito.mock(Blob.class);
		when(blob.length()).thenReturn(123L);
		when(blob.getBytes(1, (int) blob.length())).thenReturn(new byte[123]);
		var departmentEntity = DepartmentEntity.create()
			.withName("Jönssonligan")
			.withOrganizationId("123");
		var userEntity = UserEntity.create()
			.withName("John Wick");
		var attachmentEntity = AttachmentEntity.create()
			.withContent(blob)
			.withFileName("fileName.pdf")
			.withContentType("application/pdf");
		var messageEntity = MessageEntity.create()
			.withDepartment(departmentEntity)
			.withUser(userEntity)
			.withContentType("text/plain")
			.withMunicipalityId(MUNICIPALITY_ID)
			.withAttachments(List.of(attachmentEntity));
		var recipientEntity = RecipientEntity.create()
			.withPartyId("00000000-0000-0000-0000-000000000001")
			.withMessageType(MessageType.DIGITAL_MAIL);

		var messageBatchResult = new MessageBatchResult();

		when(messagingClientMock.sendDigitalMail(eq(HEADER_VALUE), eq(ORIGIN), eq(MUNICIPALITY_ID), digitalMailRequestArgumentCaptor.capture()))
			.thenReturn(messageBatchResult);

		var result = messagingIntegration.sendDigitalMail(messageEntity, recipientEntity);

		var digitalMailRequest = digitalMailRequestArgumentCaptor.getValue();
		assertThat(digitalMailRequest.getParty()).satisfies(party -> {
			assertThat(party.getPartyIds()).containsExactly(UUID.fromString("00000000-0000-0000-0000-000000000001"));
		});
		assertThat(digitalMailRequest.getAttachments()).allSatisfy(attachment -> {
			assertThat(attachment.getFilename()).isEqualTo("fileName.pdf");
			assertThat(attachment.getContentType()).isEqualTo(DigitalMailAttachment.ContentTypeEnum.APPLICATION_PDF);
		});

		assertThat(result).isNotNull().isEqualTo(messageBatchResult);
		verify(messagingClientMock).sendDigitalMail(HEADER_VALUE, ORIGIN, MUNICIPALITY_ID, digitalMailRequest);
	}

	@Test
	void sendSms() {
		var recipientEntity = RecipientEntity.create()
			.withPhoneNumber("123456789");
		var messageEntity = MessageEntity.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withDepartment(DepartmentEntity.create().withName("Jönssonligan").withOrganizationId("123"))
			.withUser(UserEntity.create().withName("John Wick"))
			.withRecipients(List.of(recipientEntity))
			.withBody("This is a text message")
			.withDisplayName("Sundsvalls Kommun");

		var messageResult = new MessageResult().messageId(UUID.randomUUID())
			.deliveries(List.of(new DeliveryResult()
				.status(MessageStatus.SENT)));
		when(messagingClientMock.sendSms(eq(HEADER_VALUE), eq(ORIGIN), eq(MUNICIPALITY_ID), smsRequestCaptor.capture()))
			.thenReturn(messageResult);

		var result = messagingIntegration.sendSms(messageEntity, recipientEntity);

		var smsRequest = smsRequestCaptor.getValue();
		assertThat(smsRequest.getMessage()).isEqualTo(messageEntity.getBody());
		assertThat(smsRequest.getMobileNumber()).isEqualTo(recipientEntity.getPhoneNumber());
		assertThat(smsRequest.getDepartment()).isEqualTo(messageEntity.getDepartment().getName());
		assertThat(smsRequest.getSender()).isEqualTo(messageEntity.getDisplayName());

		assertThat(result).isEqualTo(messageResult);
		verify(messagingClientMock).sendSms(HEADER_VALUE, ORIGIN, MUNICIPALITY_ID, smsRequest);
	}

	@Test
	void sendSmsBatch() {
		assertThat(messagingIntegration.sendSmsBatch(MUNICIPALITY_ID)).isTrue();
	}

	@Test
	void sendSnailMail() {
		var recipientEntity = RecipientEntity.create()
			.withPhoneNumber("123456789");
		var batchId = "00000000-0000-0000-0000-000000000001";
		var messageEntity = MessageEntity.create()
			.withId(batchId)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withDepartment(DepartmentEntity.create().withName("Jönssonligan").withOrganizationId("123"))
			.withUser(UserEntity.create().withName("John Wick"))
			.withRecipients(List.of(recipientEntity))
			.withBody("This is a text message")
			.withDisplayName("Sundsvalls Kommun");

		var messageResult = new MessageResult().messageId(UUID.randomUUID())
			.deliveries(List.of(new DeliveryResult()
				.status(MessageStatus.SENT)));

		when(messagingClientMock.sendSnailMail(eq(HEADER_VALUE), eq(ORIGIN), eq(MUNICIPALITY_ID), snailmailRequestCaptor.capture(), eq(batchId)))
			.thenReturn(messageResult);

		var result = messagingIntegration.sendSnailMail(messageEntity, recipientEntity);

		assertThat(result).isNotNull().isEqualTo(messageResult);

		var request = snailmailRequestCaptor.getValue();
		assertThat(request.getAttachments()).hasSameSizeAs(messageEntity.getAttachments());
		assertThat(request.getDepartment()).isEqualTo(messageEntity.getDepartment().getName());
		assertThat(request.getAddress()).satisfies(address -> {
			assertThat(address.getAddress()).isEqualTo(recipientEntity.getStreetAddress());
			assertThat(address.getCity()).isEqualTo(recipientEntity.getCity());
			assertThat(address.getCountry()).isEqualTo(recipientEntity.getCountry());
			assertThat(address.getCareOf()).isEqualTo(recipientEntity.getCareOf());
			assertThat(address.getApartmentNumber()).isEqualTo(recipientEntity.getApartmentNumber());
			assertThat(address.getFirstName()).isEqualTo(recipientEntity.getFirstName());
			assertThat(address.getLastName()).isEqualTo(recipientEntity.getLastName());
			assertThat(address.getZipCode()).isEqualTo(recipientEntity.getZipCode());
		});

		verify(messagingClientMock).sendSnailMail(HEADER_VALUE, ORIGIN, MUNICIPALITY_ID, request, batchId);
	}

}
