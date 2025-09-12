package se.sundsvall.postportalservice.integration.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.postportalservice.TestDataFactory.MUNICIPALITY_ID;

import generated.se.sundsvall.messaging.DeliveryResult;
import generated.se.sundsvall.messaging.MessageResult;
import generated.se.sundsvall.messaging.MessageStatus;
import generated.se.sundsvall.messaging.SmsRequest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.postportalservice.integration.db.DepartmentEntity;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.db.UserEntity;
import se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration;

@ExtendWith(MockitoExtension.class)
class MessagingIntegrationTest {

	@Mock
	private MessagingClient messagingClientMock;

	@Mock
	private MessagingSettingsIntegration messagingSettingsIntegrationMock;

	@Captor
	private ArgumentCaptor<SmsRequest> smsRequestCaptor;

	@InjectMocks
	private MessagingIntegration messagingIntegration;

	@AfterEach
	void verifyInteractions() {
		verifyNoMoreInteractions(messagingClientMock);
	}

	@Test
	void sendDigitalMail() {
		assertThat(messagingIntegration.sendDigitalMail(MUNICIPALITY_ID)).isTrue();
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
			.withText("This is a text message")
			.withDisplayName("Sundsvalls Kommun");

		var messageResult = new MessageResult().messageId(UUID.randomUUID())
			.deliveries(List.of(new DeliveryResult()
				.status(MessageStatus.SENT)));
		when(messagingClientMock.sendSms(eq("type=adAccount; John Wick"), eq("Jönssonligan"), eq(MUNICIPALITY_ID), smsRequestCaptor.capture()))
			.thenReturn(messageResult);

		var result = messagingIntegration.sendSms(messageEntity, recipientEntity);

		var smsRequest = smsRequestCaptor.getValue();
		assertThat(smsRequest.getMessage()).isEqualTo(messageEntity.getText());
		assertThat(smsRequest.getMobileNumber()).isEqualTo(recipientEntity.getPhoneNumber());
		assertThat(smsRequest.getDepartment()).isEqualTo(messageEntity.getDepartment().getName());
		assertThat(smsRequest.getSender()).isEqualTo(messageEntity.getDisplayName());

		assertThat(result).isEqualTo(messageResult);
		verify(messagingClientMock).sendSms("type=adAccount; John Wick", "Jönssonligan", MUNICIPALITY_ID, smsRequest);
	}

	@Test
	void sendSmsBatch() {
		assertThat(messagingIntegration.sendSmsBatch(MUNICIPALITY_ID)).isTrue();
	}

	@Test
	void sendSnailMail() {
		assertThat(messagingIntegration.sendSnailMail(MUNICIPALITY_ID)).isTrue();
	}

	@Test
	void precheckMailBoxes() {
		assertThat(messagingIntegration.precheckMailBoxes(MUNICIPALITY_ID)).isTrue();
	}

}
