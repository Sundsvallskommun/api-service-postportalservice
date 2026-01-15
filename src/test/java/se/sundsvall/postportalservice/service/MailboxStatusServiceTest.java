package se.sundsvall.postportalservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.postportalservice.TestDataFactory.MUNICIPALITY_ID;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.CONTACT_INFORMATION_EMAIL;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.CONTACT_INFORMATION_PHONE_NUMBER;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.CONTACT_INFORMATION_URL;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.DEPARTMENT_ID;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.DEPARTMENT_NAME;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.FOLDER_NAME;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.ORGANIZATION_NUMBER;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.SMS_SENDER;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.SUPPORT_TEXT;

import generated.se.sundsvall.messaging.Mailbox;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.postportalservice.integration.messaging.MessagingIntegration;
import se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration;
import se.sundsvall.postportalservice.service.util.PrecheckUtil;

@ExtendWith(MockitoExtension.class)
class MailboxStatusServiceTest {

	private static final Map<String, String> SETTINGS_MAP = Map.of(
		DEPARTMENT_ID, "departmentId",
		DEPARTMENT_NAME, "departmentName",
		ORGANIZATION_NUMBER, "123456789",
		FOLDER_NAME, "folderName",
		SMS_SENDER, "smsSender",
		SUPPORT_TEXT, "supportText",
		CONTACT_INFORMATION_URL, "contactInformationUrl",
		CONTACT_INFORMATION_PHONE_NUMBER, "contactInformationPhoneNumber",
		CONTACT_INFORMATION_EMAIL, "contactInformationEmail");

	@Mock
	private MessagingSettingsIntegration messagingSettingsMock;

	@Mock
	private MessagingIntegration messagingIntegrationMock;

	@InjectMocks
	private MailboxStatusService mailboxStatusService;

	@AfterEach
	void tearDown() {
		verifyNoMoreInteractions(messagingSettingsMock, messagingIntegrationMock);
	}

	@Test
	void testCheckMailboxStatus() {
		var partyId1 = UUID.randomUUID().toString();
		var partyId2 = UUID.randomUUID().toString();
		var partyIds = List.of(partyId1, partyId2);

		var mailboxStatuses = List.of(generateReachableMailbox(partyId1), generateReachableMailbox(partyId2));

		when(messagingSettingsMock.getMessagingSettingsForUser(MUNICIPALITY_ID)).thenReturn(SETTINGS_MAP);
		when(messagingIntegrationMock.precheckMailboxes(MUNICIPALITY_ID, SETTINGS_MAP.get(ORGANIZATION_NUMBER), partyIds))
			.thenReturn(mailboxStatuses);

		var mailboxStatus = mailboxStatusService.checkMailboxStatus(MUNICIPALITY_ID, partyIds);

		assertThat(mailboxStatus.reachable()).containsExactlyInAnyOrder(partyId1, partyId2);
		assertThat(mailboxStatus.unreachable()).isEmpty();
		assertThat(mailboxStatus.unreachableWithReason()).isEmpty();

		verify(messagingSettingsMock).getMessagingSettingsForUser(MUNICIPALITY_ID);
		verify(messagingIntegrationMock).precheckMailboxes(MUNICIPALITY_ID, SETTINGS_MAP.get(ORGANIZATION_NUMBER), partyIds);
	}

	@Test
	void testCheckMailboxStatusWithReasonWhenUnreachable() {
		var reachablePartyId = UUID.randomUUID().toString();
		var unreachablePartyId = UUID.randomUUID().toString();
		var partyIds = List.of(reachablePartyId, unreachablePartyId);

		var mailboxStatuses = List.of(generateReachableMailbox(reachablePartyId), generateUnreachableMailbox(unreachablePartyId));

		when(messagingSettingsMock.getMessagingSettingsForUser(MUNICIPALITY_ID)).thenReturn(SETTINGS_MAP);
		when(messagingIntegrationMock.precheckMailboxes(MUNICIPALITY_ID, SETTINGS_MAP.get(ORGANIZATION_NUMBER), partyIds))
			.thenReturn(mailboxStatuses);

		var mailboxStatus = mailboxStatusService.checkMailboxStatus(MUNICIPALITY_ID, partyIds);

		assertThat(mailboxStatus.reachable()).containsExactly(reachablePartyId);
		assertThat(mailboxStatus.unreachableWithReason()).extracting(PrecheckUtil.UnreachableMailbox::partyId, PrecheckUtil.UnreachableMailbox::reason)
			.containsExactly(
				tuple(unreachablePartyId, "Some reason"));

		verify(messagingSettingsMock).getMessagingSettingsForUser(MUNICIPALITY_ID);
		verify(messagingIntegrationMock).precheckMailboxes(MUNICIPALITY_ID, SETTINGS_MAP.get(ORGANIZATION_NUMBER), partyIds);

	}

	private Mailbox generateReachableMailbox(final String partyId) {
		return new Mailbox()
			.partyId(partyId)
			.reachable(true);
	}

	private Mailbox generateUnreachableMailbox(final String partyId) {
		return new Mailbox()
			.partyId(partyId)
			.reachable(false)
			.reason("Some reason");
	}
}
