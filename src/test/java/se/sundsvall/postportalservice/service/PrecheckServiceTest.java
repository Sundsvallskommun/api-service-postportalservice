package se.sundsvall.postportalservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.BAD_GATEWAY;
import static se.sundsvall.postportalservice.TestDataFactory.createMailbox;

import generated.se.sundsvall.citizen.CitizenAddress;
import generated.se.sundsvall.citizen.CitizenExtended;
import generated.se.sundsvall.citizen.PersonGuidBatch;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.Problem;
import se.sundsvall.postportalservice.api.model.PrecheckResponse.DeliveryMethod;
import se.sundsvall.postportalservice.api.model.PrecheckResponse.PrecheckRecipient;
import se.sundsvall.postportalservice.integration.citizen.CitizenIntegration;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.db.converter.MessageType;
import se.sundsvall.postportalservice.integration.messaging.MessagingIntegration;
import se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration;
import se.sundsvall.postportalservice.service.mapper.EntityMapper;
import se.sundsvall.postportalservice.service.mapper.PrecheckMapper;

@ExtendWith(MockitoExtension.class)
class PrecheckServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String ORGANIZATION_NUMBER = "5555555555";
	private static final String DEPARTMENT_ID = "123";

	@Mock
	private CitizenIntegration citizenIntegrationMock;

	@Mock
	private EmployeeService employeeServiceMock;

	@Mock
	private MessagingSettingsIntegration messagingSettingsIntegrationMock;

	@Mock
	private MessagingIntegration messagingIntegrationMock;

	@Mock
	private PrecheckMapper precheckMapperMock;

	@Mock(answer = Answers.CALLS_REAL_METHODS)
	private EntityMapper entityMapperMock;

	@InjectMocks
	private PrecheckService precheckService;

	@AfterEach
	void noMoreInteractions() {
		verifyNoMoreInteractions(citizenIntegrationMock, messagingSettingsIntegrationMock, messagingIntegrationMock, entityMapperMock, precheckMapperMock);
	}

	@Test
	void precheckPartyIds() {
		final var partyId1 = "5c1b2636-5ffc-467d-95be-156aeb73ec8e"; // Eligible for digital mail
		final var partyId2 = "7c1b2636-5ffc-467d-95be-156aeb73ec8e"; // Not eligible for digital mail, but eligible for snail mail
		final var partyId3 = "8c1b2636-5ffc-467d-95be-156aeb73ec8e"; // Not eligible for digital mail or snail mail
		final var partyIds = List.of(partyId1, partyId2, partyId3);

		final var mailbox1 = createMailbox(partyId1, true);
		final var mailbox2 = createMailbox(partyId2, false);
		final var mailbox3 = createMailbox(partyId3, false);
		final var mailboxes = List.of(mailbox1, mailbox2, mailbox3);

		final var citizenExtended2 = new CitizenExtended()
			.addresses(List.of(new CitizenAddress().addressType("POPULATION_REGISTRATION_ADDRESS")));
		final var citizenExtended3 = new CitizenExtended()
			.addresses(List.of(new CitizenAddress().addressType("INVALID_ADDRESS_TYPE")));

		final var citizens = List.of(citizenExtended2, citizenExtended3);

		final var sentBy = new EmployeeService.SentBy("username", DEPARTMENT_ID, "departmentName");

		when(employeeServiceMock.getSentBy(MUNICIPALITY_ID)).thenReturn(sentBy);
		when(messagingSettingsIntegrationMock.getOrganizationNumber(MUNICIPALITY_ID, DEPARTMENT_ID)).thenReturn(ORGANIZATION_NUMBER);
		when(messagingIntegrationMock.precheckMailboxes(MUNICIPALITY_ID, ORGANIZATION_NUMBER, partyIds)).thenReturn(mailboxes);
		when(citizenIntegrationMock.getCitizens(MUNICIPALITY_ID, List.of(partyId2, partyId3))).thenReturn(citizens);
		when(precheckMapperMock.toSnailMailEligiblePartyIds(eq(citizens), ArgumentMatchers.<Predicate<CitizenExtended>>any())).thenReturn(List.of(partyId2));
		when(precheckMapperMock.toSnailMailIneligiblePartyIds(eq(citizens), ArgumentMatchers.<Predicate<CitizenExtended>>any())).thenReturn(List.of(partyId3));

		final var result = precheckService.precheckPartyIds(MUNICIPALITY_ID, partyIds);

		assertThat(result.precheckRecipients()).hasSize(3)
			.extracting(PrecheckRecipient::partyId, PrecheckRecipient::deliveryMethod)
			.containsExactlyInAnyOrder(
				tuple(partyId1, DeliveryMethod.DIGITAL_MAIL),
				tuple(partyId2, DeliveryMethod.SNAIL_MAIL),
				tuple(partyId3, DeliveryMethod.DELIVERY_NOT_POSSIBLE));

		verify(employeeServiceMock).getSentBy(MUNICIPALITY_ID);
		verify(messagingIntegrationMock).precheckMailboxes(MUNICIPALITY_ID, ORGANIZATION_NUMBER, partyIds);
		verify(citizenIntegrationMock).getCitizens(MUNICIPALITY_ID, List.of(partyId2, partyId3));
		verify(precheckMapperMock).toSnailMailEligiblePartyIds(eq(citizens), ArgumentMatchers.<Predicate<CitizenExtended>>any());
		verify(precheckMapperMock).toSnailMailIneligiblePartyIds(eq(citizens), ArgumentMatchers.<Predicate<CitizenExtended>>any());
		verifyNoMoreInteractions(employeeServiceMock, citizenIntegrationMock, messagingIntegrationMock, precheckMapperMock);
	}

	@Test
	void precheckPartyIds_employeeThrows() {
		when(employeeServiceMock.getSentBy(MUNICIPALITY_ID)).thenThrow(Problem.valueOf(BAD_GATEWAY, "Failed to retrieve employee data"));

		assertThatThrownBy(() -> precheckService.precheckPartyIds(MUNICIPALITY_ID, List.of()))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Bad Gateway: Failed to retrieve employee data");

		verify(employeeServiceMock).getSentBy(MUNICIPALITY_ID);
		verifyNoMoreInteractions(employeeServiceMock);
		verifyNoInteractions(messagingIntegrationMock, citizenIntegrationMock, precheckMapperMock);
	}

	@Test
	void precheckLegalIds() {
		// Person 1 - Eligible for digital mail
		final var legalId1 = "201801022383";
		final var partyId1 = "5c1b2636-5ffc-467d-95be-156aeb73ec8e";
		final var personGuidBatch1 = new PersonGuidBatch()
			.success(Boolean.TRUE)
			.personNumber(legalId1)
			.personId(UUID.fromString(partyId1));

		// Person 2 - Eligible for snail mail
		final var legalId2 = "201801032390";
		final var partyId2 = "7c1b2636-5ffc-467d-95be-156aeb73ec8e";
		final var personGuidBatch2 = new PersonGuidBatch()
			.success(Boolean.TRUE)
			.personNumber(legalId2)
			.personId(UUID.fromString(partyId2));

		// Person 3 - Not eligible for digital mail or snail mail
		final var legalId3 = "201801042381";
		final var partyId3 = "8c1b2636-5ffc-467d-95be-156aeb73ec8e";
		final var personGuidBatch3 = new PersonGuidBatch()
			.success(Boolean.TRUE)
			.personNumber(legalId3)
			.personId(UUID.fromString(partyId3));

		final var legalIds = List.of(legalId1, legalId2, legalId3);
		final var personGuidBatches = List.of(personGuidBatch1, personGuidBatch2, personGuidBatch3);
		final var partyIds = List.of(partyId1, partyId2, partyId3);

		when(citizenIntegrationMock.getPartyIds(MUNICIPALITY_ID, legalIds)).thenReturn(personGuidBatches);
		final var sentBy = new EmployeeService.SentBy("username", DEPARTMENT_ID, "departmentName");

		when(employeeServiceMock.getSentBy(MUNICIPALITY_ID)).thenReturn(sentBy);
		when(messagingSettingsIntegrationMock.getOrganizationNumber(MUNICIPALITY_ID, DEPARTMENT_ID)).thenReturn(ORGANIZATION_NUMBER);

		final var mailbox1 = createMailbox(partyId1, true);
		final var mailbox2 = createMailbox(partyId2, false);
		final var mailbox3 = createMailbox(partyId3, false);
		final var mailboxes = List.of(mailbox1, mailbox2, mailbox3);
		when(messagingIntegrationMock.precheckMailboxes(MUNICIPALITY_ID, ORGANIZATION_NUMBER, partyIds)).thenReturn(mailboxes);

		final var citizenExtended1 = new CitizenExtended()
			.personId(UUID.fromString(partyId2))
			.addresses(List.of(new CitizenAddress()
				.status("Current")
				.addressType("POPULATION_REGISTRATION_ADDRESS")));
		final var citizenExtended2 = new CitizenExtended()
			.personId(UUID.fromString(partyId3))
			.addresses(List.of(new CitizenAddress()
				.status("Current")
				.addressType("NOT_ELIGIBLE_ADDRESS_TYPE")));
		final var citizenExtendedList = List.of(citizenExtended1, citizenExtended2);
		when(citizenIntegrationMock.getCitizens(MUNICIPALITY_ID, List.of(partyId2, partyId3))).thenReturn(citizenExtendedList);
		when(citizenIntegrationMock.isRegisteredInSweden(citizenExtended1)).thenReturn(true);
		when(citizenIntegrationMock.isRegisteredInSweden(citizenExtended2)).thenReturn(false);

		final var result = precheckService.precheckLegalIds(MUNICIPALITY_ID, legalIds);

		assertThat(result).hasSize(3).extracting(
			RecipientEntity::getPartyId, RecipientEntity::getMessageType, RecipientEntity::getStatus)
			.containsExactlyInAnyOrder(
				tuple(partyId1, MessageType.DIGITAL_MAIL, "PENDING"),
				tuple(partyId2, MessageType.SNAIL_MAIL, "PENDING"),
				tuple(partyId3, MessageType.LETTER, "UNDELIVERABLE"));

		verify(citizenIntegrationMock).getPartyIds(MUNICIPALITY_ID, legalIds);
		verify(employeeServiceMock).getSentBy(MUNICIPALITY_ID);
		verify(messagingSettingsIntegrationMock).getOrganizationNumber(MUNICIPALITY_ID, DEPARTMENT_ID);
		verify(messagingIntegrationMock).precheckMailboxes(MUNICIPALITY_ID, ORGANIZATION_NUMBER, partyIds);
		verify(citizenIntegrationMock).getCitizens(MUNICIPALITY_ID, List.of(partyId2, partyId3));
		verify(entityMapperMock).toDigitalMailRecipientEntity(any());
		verify(entityMapperMock).toSnailMailRecipientEntity(any());
		verify(entityMapperMock).toUndeliverableRecipientEntity(any());
		verifyNoMoreInteractions(citizenIntegrationMock, employeeServiceMock, messagingSettingsIntegrationMock, messagingIntegrationMock);
	}

	@Test
	void precheckLegalIds_citizenThrows() {
		final var legalIds = List.of("201801022383", "201801032390", "201801042381");
		when(citizenIntegrationMock.getPartyIds(MUNICIPALITY_ID, legalIds)).thenThrow(Problem.valueOf(BAD_GATEWAY, "Failed to retrieve citizen data"));

		assertThatThrownBy(() -> precheckService.precheckLegalIds(MUNICIPALITY_ID, legalIds))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Bad Gateway: Failed to retrieve citizen data");

		verify(citizenIntegrationMock).getPartyIds(MUNICIPALITY_ID, legalIds);
		verifyNoInteractions(employeeServiceMock, messagingSettingsIntegrationMock, messagingIntegrationMock, precheckMapperMock);
	}

	@Test
	void precheckLegalIds_employeeThrows() {
		final var legalIds = List.of("201801022383", "201801032390", "201801042381");
		final var batches = List.of(new PersonGuidBatch(), new PersonGuidBatch());
		when(citizenIntegrationMock.getPartyIds(MUNICIPALITY_ID, legalIds)).thenReturn(batches);

		when(employeeServiceMock.getSentBy(MUNICIPALITY_ID)).thenThrow(Problem.valueOf(BAD_GATEWAY, "Failed to retrieve employee data"));

		assertThatThrownBy(() -> precheckService.precheckLegalIds(MUNICIPALITY_ID, legalIds))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Bad Gateway: Failed to retrieve employee data");

		verify(citizenIntegrationMock).getPartyIds(MUNICIPALITY_ID, legalIds);
		verify(employeeServiceMock).getSentBy(MUNICIPALITY_ID);
		verifyNoMoreInteractions(citizenIntegrationMock, employeeServiceMock);
		verifyNoInteractions(messagingSettingsIntegrationMock, messagingIntegrationMock, precheckMapperMock);
	}

	@Test
	void precheckLegalIds_messagingSettingThrows() {
		final var legalIds = List.of("201801022383", "201801032390", "201801042381");
		final var batches = List.of(new PersonGuidBatch(), new PersonGuidBatch());
		final var sentBy = new EmployeeService.SentBy("username", DEPARTMENT_ID, "departmentName");

		when(citizenIntegrationMock.getPartyIds(MUNICIPALITY_ID, legalIds)).thenReturn(batches);
		when(employeeServiceMock.getSentBy(MUNICIPALITY_ID)).thenReturn(sentBy);
		when(messagingSettingsIntegrationMock.getOrganizationNumber(MUNICIPALITY_ID, DEPARTMENT_ID))
			.thenThrow(Problem.valueOf(BAD_GATEWAY, "Failed to retrieve messaging settings"));

		assertThatThrownBy(() -> precheckService.precheckLegalIds(MUNICIPALITY_ID, legalIds))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Bad Gateway: Failed to retrieve messaging settings");

		verify(citizenIntegrationMock).getPartyIds(MUNICIPALITY_ID, legalIds);
		verify(employeeServiceMock).getSentBy(MUNICIPALITY_ID);
		verify(messagingSettingsIntegrationMock).getOrganizationNumber(MUNICIPALITY_ID, DEPARTMENT_ID);
		verifyNoMoreInteractions(citizenIntegrationMock, employeeServiceMock, messagingSettingsIntegrationMock);
		verifyNoInteractions(messagingIntegrationMock, precheckMapperMock);
	}

	@Test
	void precheckLegalIds_messagingThrows() {
		final var legalIds = List.of("201801022383", "201801032390", "201801042381");
		final var batches = List.of(new PersonGuidBatch(), new PersonGuidBatch());
		final var sentBy = new EmployeeService.SentBy("username", DEPARTMENT_ID, "departmentName");

		when(citizenIntegrationMock.getPartyIds(MUNICIPALITY_ID, legalIds)).thenReturn(batches);
		when(employeeServiceMock.getSentBy(MUNICIPALITY_ID)).thenReturn(sentBy);
		when(messagingSettingsIntegrationMock.getOrganizationNumber(MUNICIPALITY_ID, DEPARTMENT_ID)).thenReturn(ORGANIZATION_NUMBER);
		when(messagingIntegrationMock.precheckMailboxes(MUNICIPALITY_ID, ORGANIZATION_NUMBER, List.of())).thenThrow(Problem.valueOf(BAD_GATEWAY, "Failed to retrieve mailbox data"));

		assertThatThrownBy(() -> precheckService.precheckLegalIds(MUNICIPALITY_ID, legalIds))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Bad Gateway: Failed to retrieve mailbox data");

		verify(citizenIntegrationMock).getPartyIds(MUNICIPALITY_ID, legalIds);
		verify(employeeServiceMock).getSentBy(MUNICIPALITY_ID);
		verify(messagingSettingsIntegrationMock).getOrganizationNumber(MUNICIPALITY_ID, DEPARTMENT_ID);
		verify(messagingIntegrationMock).precheckMailboxes(MUNICIPALITY_ID, ORGANIZATION_NUMBER, List.of());
		verifyNoMoreInteractions(citizenIntegrationMock, employeeServiceMock, messagingSettingsIntegrationMock, messagingIntegrationMock);
		verifyNoInteractions(precheckMapperMock);
	}

}
