package se.sundsvall.postportalservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.postportalservice.TestDataFactory.MUNICIPALITY_ID;
import static se.sundsvall.postportalservice.TestDataFactory.SUNDSVALL_MUNICIPALITY_ORG_NO;

import generated.se.sundsvall.messaging.Mailbox;
import java.util.List;
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

	@Mock
	private EmployeeService employeeServiceMock;

	@Mock
	private MessagingSettingsIntegration messagingSettingsMock;

	@Mock
	private MessagingIntegration messagingIntegrationMock;

	@InjectMocks
	private MailboxStatusService mailboxStatusService;

	@AfterEach
	void tearDown() {
		verifyNoMoreInteractions(employeeServiceMock, messagingSettingsMock, messagingIntegrationMock);
	}

	@Test
	void testCheckMailboxStatus() {
		var partyId1 = UUID.randomUUID().toString();
		var partyId2 = UUID.randomUUID().toString();
		var partyIds = List.of(partyId1, partyId2);

		var mailboxStatuses = List.of(generateReachableMailbox(partyId1), generateReachableMailbox(partyId2));

		var sentBy = new EmployeeService.SentBy("userName", "deptId", "deptName");
		when(employeeServiceMock.getSentBy(MUNICIPALITY_ID)).thenReturn(sentBy);
		when(messagingSettingsMock.getOrganizationNumber(MUNICIPALITY_ID, sentBy.departmentId())).thenReturn(SUNDSVALL_MUNICIPALITY_ORG_NO);
		when(messagingIntegrationMock.precheckMailboxes(MUNICIPALITY_ID, SUNDSVALL_MUNICIPALITY_ORG_NO, partyIds))
			.thenReturn(mailboxStatuses);

		var mailboxStatus = mailboxStatusService.checkMailboxStatus(MUNICIPALITY_ID, partyIds);

		assertThat(mailboxStatus.reachable()).containsExactlyInAnyOrder(partyId1, partyId2);
		assertThat(mailboxStatus.unreachable()).isEmpty();
		assertThat(mailboxStatus.unreachableWithReason()).isEmpty();

		verify(employeeServiceMock).getSentBy(MUNICIPALITY_ID);
		verify(messagingSettingsMock).getOrganizationNumber(MUNICIPALITY_ID, sentBy.departmentId());
		verify(messagingIntegrationMock).precheckMailboxes(MUNICIPALITY_ID, SUNDSVALL_MUNICIPALITY_ORG_NO, partyIds);
	}

	@Test
	void testCheckMailboxStatusWithReasonWhenUnreachable() {
		var reachablePartyId = UUID.randomUUID().toString();
		var unreachablePartyId = UUID.randomUUID().toString();
		var partyIds = List.of(reachablePartyId, unreachablePartyId);

		var mailboxStatuses = List.of(generateReachableMailbox(reachablePartyId), generateUnreachableMailbox(unreachablePartyId));

		var sentBy = new EmployeeService.SentBy("userName", "deptId", "deptName");
		when(employeeServiceMock.getSentBy(MUNICIPALITY_ID)).thenReturn(sentBy);
		when(messagingSettingsMock.getOrganizationNumber(MUNICIPALITY_ID, sentBy.departmentId())).thenReturn(SUNDSVALL_MUNICIPALITY_ORG_NO);
		when(messagingIntegrationMock.precheckMailboxes(MUNICIPALITY_ID, SUNDSVALL_MUNICIPALITY_ORG_NO, partyIds))
			.thenReturn(mailboxStatuses);

		var mailboxStatus = mailboxStatusService.checkMailboxStatus(MUNICIPALITY_ID, partyIds);

		assertThat(mailboxStatus.reachable()).containsExactly(reachablePartyId);
		assertThat(mailboxStatus.unreachableWithReason()).extracting(PrecheckUtil.UnreachableMailbox::partyId, PrecheckUtil.UnreachableMailbox::reason)
			.containsExactly(
				tuple(unreachablePartyId, "Some reason"));

		verify(employeeServiceMock).getSentBy(MUNICIPALITY_ID);
		verify(messagingSettingsMock).getOrganizationNumber(MUNICIPALITY_ID, sentBy.departmentId());
		verify(messagingIntegrationMock).precheckMailboxes(MUNICIPALITY_ID, SUNDSVALL_MUNICIPALITY_ORG_NO, partyIds);

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
