package se.sundsvall.postportalservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.BAD_GATEWAY;
import static se.sundsvall.postportalservice.TestDataFactory.generateLegalId;

import generated.se.sundsvall.citizen.CitizenAddress;
import generated.se.sundsvall.citizen.CitizenExtended;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import org.zalando.problem.Problem;
import se.sundsvall.dept44.test.annotation.resource.Load;
import se.sundsvall.dept44.test.extension.ResourceLoaderExtension;
import se.sundsvall.postportalservice.api.model.PrecheckResponse.DeliveryMethod;
import se.sundsvall.postportalservice.api.model.PrecheckResponse.PrecheckRecipient;
import se.sundsvall.postportalservice.integration.citizen.CitizenIntegration;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.db.converter.MessageType;
import se.sundsvall.postportalservice.integration.digitalregisteredletter.DigitalRegisteredLetterIntegration;
import se.sundsvall.postportalservice.integration.party.PartyIntegration;
import se.sundsvall.postportalservice.service.MailboxStatusService.MailboxStatus;
import se.sundsvall.postportalservice.service.mapper.EntityMapper;
import se.sundsvall.postportalservice.service.util.PrecheckUtil;

@ExtendWith({
	MockitoExtension.class, ResourceLoaderExtension.class
})
class PrecheckServiceTest {

	private static final String MUNICIPALITY_ID = "2281";

	@Mock
	private CitizenIntegration citizenIntegrationMock;

	@Mock
	private MailboxStatusService mailboxStatusServiceMock;

	@Mock
	private DigitalRegisteredLetterIntegration digitalRegisteredLetterIntegrationMock;

	@Mock
	private PartyIntegration partyIntegrationMock;

	@Mock(answer = Answers.CALLS_REAL_METHODS)
	private EntityMapper entityMapperMock;

	@InjectMocks
	private PrecheckService precheckService;

	@AfterEach
	void noMoreInteractions() {
		verifyNoMoreInteractions(citizenIntegrationMock, entityMapperMock, mailboxStatusServiceMock,
			digitalRegisteredLetterIntegrationMock, partyIntegrationMock);
	}

	@Test
	void precheckPartyIds() {
		final var digitalMailUuid = "5c1b2636-5ffc-467d-95be-156aeb73ec8e"; // Eligible for digital mail
		final var snailMailUuid = "7c1b2636-5ffc-467d-95be-156aeb73ec8e"; // Not eligible for digital mail, but eligible for snail mail
		final var notEligibleUuid = "8c1b2636-5ffc-467d-95be-156aeb73ec8e"; // Not eligible for digital mail or snail mail
		final var partyIds = List.of(digitalMailUuid, snailMailUuid, notEligibleUuid);

		// Mock mailbox status - digitalMailUuid is reachable, others are not
		final var unreachableMailbox1 = new PrecheckUtil.UnreachableMailbox(snailMailUuid, "No digital mailbox");
		final var unreachableMailbox2 = new PrecheckUtil.UnreachableMailbox(notEligibleUuid, "No digital mailbox");
		final var mailboxStatus = new MailboxStatus(
			List.of(digitalMailUuid),
			List.of(unreachableMailbox1, unreachableMailbox2));

		when(mailboxStatusServiceMock.checkMailboxStatus(MUNICIPALITY_ID, partyIds)).thenReturn(mailboxStatus);

		// Mock citizen data for those without digital mailboxes
		final var snailMailCitizen = new CitizenExtended()
			.personId(UUID.fromString(snailMailUuid))
			.addresses(List.of(new CitizenAddress().addressType("POPULATION_REGISTRATION_ADDRESS")));
		final var notEligibleCitizen = new CitizenExtended()
			.personId(UUID.fromString(notEligibleUuid))
			.addresses(List.of(new CitizenAddress().addressType("INVALID_ADDRESS_TYPE")));
		final var digitalMailCitizen = new CitizenExtended()
			.personId(UUID.fromString(digitalMailUuid))
			.addresses(List.of(new CitizenAddress().addressType("POPULATION_REGISTRATION_ADDRESS")));

		final var citizens = List.of(digitalMailCitizen, snailMailCitizen, notEligibleCitizen);

		when(citizenIntegrationMock.getCitizens(MUNICIPALITY_ID, partyIds)).thenReturn(citizens);

		// Mock person numbers for age verification
		final var partyIdToLegalIdMap = Map.of(
			digitalMailUuid, generateLegalId(30, "2399"),
			snailMailUuid, generateLegalId(35, "2398"),
			notEligibleUuid, generateLegalId(40, "2388"));

		when(partyIntegrationMock.getLegalIds(MUNICIPALITY_ID, partyIds))
			.thenReturn(partyIdToLegalIdMap);

		final var result = precheckService.precheckPartyIds(MUNICIPALITY_ID, partyIds);

		assertThat(result.precheckRecipients()).hasSize(3)
			.extracting(PrecheckRecipient::partyId, PrecheckRecipient::deliveryMethod)
			.containsExactlyInAnyOrder(
				tuple(digitalMailUuid, DeliveryMethod.DIGITAL_MAIL),
				tuple(snailMailUuid, DeliveryMethod.SNAIL_MAIL),
				tuple(notEligibleUuid, DeliveryMethod.DELIVERY_NOT_POSSIBLE));

		verify(mailboxStatusServiceMock).checkMailboxStatus(MUNICIPALITY_ID, partyIds);
		verify(citizenIntegrationMock).getCitizens(MUNICIPALITY_ID, partyIds);
		verify(partyIntegrationMock).getLegalIds(MUNICIPALITY_ID, partyIds);
	}

	@Test
	void precheckPartyIds_mailboxServiceThrows() {
		final var partyIds = List.of("party-1", "party-2");

		when(mailboxStatusServiceMock.checkMailboxStatus(MUNICIPALITY_ID, partyIds))
			.thenThrow(Problem.valueOf(BAD_GATEWAY, "Failed to check mailbox status"));

		assertThatThrownBy(() -> precheckService.precheckPartyIds(MUNICIPALITY_ID, partyIds))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Bad Gateway: Failed to check mailbox status");

		verify(mailboxStatusServiceMock).checkMailboxStatus(MUNICIPALITY_ID, partyIds);
		verifyNoInteractions(citizenIntegrationMock);
	}

	@Test
	void precheckPartyIds_minorWithDigitalMailbox() {
		final var minorUuid = "1c1b2636-5ffc-467d-95be-156aeb73ec8e"; // Minor with digital mailbox
		final var adultUuid = "2c1b2636-5ffc-467d-95be-156aeb73ec8e"; // Adult with digital mailbox
		final var partyIds = List.of(minorUuid, adultUuid);

		// Mock mailbox status - both are reachable via digital mail
		final var mailboxStatus = new MailboxStatus(List.of(minorUuid, adultUuid), List.of());

		when(mailboxStatusServiceMock.checkMailboxStatus(MUNICIPALITY_ID, partyIds)).thenReturn(mailboxStatus);

		// Mock citizen data for age verification
		final var minorCitizen = new CitizenExtended()
			.personId(UUID.fromString(minorUuid))
			.addresses(List.of(new CitizenAddress().addressType("POPULATION_REGISTRATION_ADDRESS")));
		final var adultCitizen = new CitizenExtended()
			.personId(UUID.fromString(adultUuid))
			.addresses(List.of(new CitizenAddress().addressType("POPULATION_REGISTRATION_ADDRESS")));

		final var citizens = List.of(minorCitizen, adultCitizen);

		when(citizenIntegrationMock.getCitizens(MUNICIPALITY_ID, partyIds)).thenReturn(citizens);

		// Mock person numbers for age verification - minor is 15 years old, adult is 30 years old
		final var partyIdToLegalIdMap = Map.of(
			minorUuid, generateLegalId(15, "2399"),
			adultUuid, generateLegalId(30, "2398"));

		when(partyIntegrationMock.getLegalIds(MUNICIPALITY_ID, partyIds))
			.thenReturn(partyIdToLegalIdMap);

		final var result = precheckService.precheckPartyIds(MUNICIPALITY_ID, partyIds);

		// Verify that minor is marked as ineligible and adult can use digital mail
		assertThat(result.precheckRecipients()).hasSize(2)
			.extracting(PrecheckRecipient::partyId, PrecheckRecipient::deliveryMethod, PrecheckRecipient::reason)
			.containsExactlyInAnyOrder(
				tuple(adultUuid, DeliveryMethod.DIGITAL_MAIL, null),
				tuple(minorUuid, DeliveryMethod.DELIVERY_NOT_POSSIBLE, "INELIGIBLE_MINOR"));

		verify(mailboxStatusServiceMock).checkMailboxStatus(MUNICIPALITY_ID, partyIds);
		verify(citizenIntegrationMock).getCitizens(MUNICIPALITY_ID, partyIds);
		verify(partyIntegrationMock).getLegalIds(MUNICIPALITY_ID, partyIds);
	}

	@Test
	void precheckLegalIds_adultWithDigitalMailbox() {
		final var legalId = generateLegalId(35, "0000");
		final var partyId = UUID.randomUUID().toString();

		final var legalIds = List.of(legalId);
		final var partyIds = List.of(partyId);
		final var legalIdToPartyIdMap = Map.of(legalId, partyId);

		when(partyIntegrationMock.getPartyIds(MUNICIPALITY_ID, legalIds)).thenReturn(legalIdToPartyIdMap);

		// Mock mailbox status - digital mailbox available
		final var mailboxStatus = new MailboxStatus(List.of(partyId), List.of());
		when(mailboxStatusServiceMock.checkMailboxStatus(MUNICIPALITY_ID, partyIds)).thenReturn(mailboxStatus);

		final var result = precheckService.precheckLegalIds(MUNICIPALITY_ID, legalIds);

		assertThat(result).hasSize(1)
			.extracting(RecipientEntity::getPartyId, RecipientEntity::getMessageType, RecipientEntity::getStatus)
			.containsExactly(tuple(partyId, MessageType.DIGITAL_MAIL, "PENDING"));

		// Verify
		verify(partyIntegrationMock).getPartyIds(MUNICIPALITY_ID, legalIds);
		verify(mailboxStatusServiceMock).checkMailboxStatus(MUNICIPALITY_ID, partyIds);
		verify(entityMapperMock).toDigitalMailRecipientEntity(any());
	}

	@Test
	void precheckLegalIds_adultWithoutMailboxRegistered() {
		final var legalId = generateLegalId(40, "2390");
		final var partyId = UUID.randomUUID().toString();

		final var legalIds = List.of(legalId);
		final var partyIds = List.of(partyId);
		final var legalIdToPartyIdMap = Map.of(legalId, partyId);

		when(partyIntegrationMock.getPartyIds(MUNICIPALITY_ID, legalIds)).thenReturn(legalIdToPartyIdMap);

		// Mock mailbox status - no digital mailbox
		final var unreachableMailbox = new PrecheckUtil.UnreachableMailbox(partyId, "No digital mailbox");
		final var mailboxStatus = new MailboxStatus(List.of(), List.of(unreachableMailbox));
		when(mailboxStatusServiceMock.checkMailboxStatus(MUNICIPALITY_ID, partyIds)).thenReturn(mailboxStatus);

		// adult registered in Sweden
		final var citizenExtended = new CitizenExtended()
			.personId(UUID.fromString(partyId))
			.addresses(List.of(new CitizenAddress()
				.addressType("POPULATION_REGISTRATION_ADDRESS")));

		when(citizenIntegrationMock.getCitizens(MUNICIPALITY_ID, partyIds)).thenReturn(List.of(citizenExtended));

		final var result = precheckService.precheckLegalIds(MUNICIPALITY_ID, legalIds);

		assertThat(result).hasSize(1)
			.extracting(RecipientEntity::getPartyId, RecipientEntity::getMessageType, RecipientEntity::getStatus)
			.containsExactly(tuple(partyId, MessageType.SNAIL_MAIL, "PENDING"));

		verify(partyIntegrationMock).getPartyIds(MUNICIPALITY_ID, legalIds);
		verify(mailboxStatusServiceMock).checkMailboxStatus(MUNICIPALITY_ID, partyIds);
		verify(citizenIntegrationMock).getCitizens(MUNICIPALITY_ID, partyIds);
		verify(entityMapperMock).toSnailMailRecipientEntity(any());
	}

	@Test
	void precheckLegalIds_notRegisteredInSweden() {
		final var legalId = generateLegalId(45, "2381");
		final var partyId = UUID.randomUUID().toString();

		final var legalIds = List.of(legalId);
		final var partyIds = List.of(partyId);
		final var legalIdToPartyIdMap = Map.of(legalId, partyId);

		when(partyIntegrationMock.getPartyIds(MUNICIPALITY_ID, legalIds)).thenReturn(legalIdToPartyIdMap);

		// Mock mailbox status - no digital mailbox
		final var unreachableMailbox = new PrecheckUtil.UnreachableMailbox(partyId, "No digital mailbox");
		final var mailboxStatus = new MailboxStatus(List.of(), List.of(unreachableMailbox));
		when(mailboxStatusServiceMock.checkMailboxStatus(MUNICIPALITY_ID, partyIds)).thenReturn(mailboxStatus);

		// Setup citizen data - NOT registered in Sweden
		final var citizenExtended = new CitizenExtended()
			.personId(UUID.fromString(partyId))
			.addresses(List.of(new CitizenAddress()
				.addressType("NOT_POPULATION_REGISTRATION_ADDRESS")));

		when(citizenIntegrationMock.getCitizens(MUNICIPALITY_ID, partyIds))
			.thenReturn(List.of(citizenExtended));

		// Execute
		final var result = precheckService.precheckLegalIds(MUNICIPALITY_ID, legalIds);

		// Assert
		assertThat(result).hasSize(1)
			.extracting(RecipientEntity::getPartyId, RecipientEntity::getMessageType, RecipientEntity::getStatus)
			.containsExactly(tuple(partyId, MessageType.LETTER, "UNDELIVERABLE"));

		// Verify
		verify(partyIntegrationMock).getPartyIds(MUNICIPALITY_ID, legalIds);
		verify(mailboxStatusServiceMock).checkMailboxStatus(MUNICIPALITY_ID, partyIds);
		verify(citizenIntegrationMock).getCitizens(MUNICIPALITY_ID, partyIds);
		verify(entityMapperMock).toUndeliverableRecipientEntity(any());
	}

	@Test
	void precheckLegalIds_minorIneligible() {
		// Generate legal ID for 15-year-old minor
		final var legalId = generateLegalId(15, "2387");
		final var partyId = UUID.randomUUID().toString();

		final var legalIds = List.of(legalId);
		final var partyIds = List.of(partyId);
		final var legalIdToPartyIdMap = Map.of(legalId, partyId);

		// Setup mocks
		when(partyIntegrationMock.getPartyIds(MUNICIPALITY_ID, legalIds)).thenReturn(legalIdToPartyIdMap);

		// Mock mailbox status - no digital mailbox
		final var unreachableMailbox = new PrecheckUtil.UnreachableMailbox(partyId, "No digital mailbox");
		final var mailboxStatus = new MailboxStatus(List.of(), List.of(unreachableMailbox));
		when(mailboxStatusServiceMock.checkMailboxStatus(MUNICIPALITY_ID, partyIds)).thenReturn(mailboxStatus);

		// Setup citizen data - minor registered in Sweden
		final var citizenExtended = new CitizenExtended()
			.personId(UUID.fromString(partyId))
			.addresses(List.of(new CitizenAddress()
				.addressType("POPULATION_REGISTRATION_ADDRESS")));

		when(citizenIntegrationMock.getCitizens(MUNICIPALITY_ID, partyIds))
			.thenReturn(List.of(citizenExtended));

		// Execute
		final var result = precheckService.precheckLegalIds(MUNICIPALITY_ID, legalIds);

		// Assert
		assertThat(result).hasSize(1)
			.extracting(RecipientEntity::getPartyId, RecipientEntity::getMessageType, RecipientEntity::getStatus)
			.containsExactly(tuple(partyId, MessageType.LETTER, "INELIGIBLE_MINOR"));

		// Verify
		verify(partyIntegrationMock).getPartyIds(MUNICIPALITY_ID, legalIds);
		verify(mailboxStatusServiceMock).checkMailboxStatus(MUNICIPALITY_ID, partyIds);
		verify(citizenIntegrationMock).getCitizens(MUNICIPALITY_ID, partyIds);
		verify(entityMapperMock).toIneligibleMinorRecipientEntity(any());
	}

	@Test
	void precheckLegalIds_partyIntegrationThrows() {
		final var legalIds = List.of("201801022383", "201801032390", "201801042381");
		when(partyIntegrationMock.getPartyIds(MUNICIPALITY_ID, legalIds)).thenThrow(Problem.valueOf(BAD_GATEWAY, "Failed to retrieve party data"));

		assertThatThrownBy(() -> precheckService.precheckLegalIds(MUNICIPALITY_ID, legalIds))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Bad Gateway: Failed to retrieve party data");

		verify(partyIntegrationMock).getPartyIds(MUNICIPALITY_ID, legalIds);
		verifyNoInteractions(mailboxStatusServiceMock);
	}

	@Test
	void precheckLegalIds_mailboxServiceThrows() {
		final var legalIds = List.of("201801022383", "201801032390", "201801042381");
		final var partyId = UUID.randomUUID().toString();
		final var legalIdToPartyIdMap = Map.of(legalIds.getFirst(), partyId);

		when(partyIntegrationMock.getPartyIds(MUNICIPALITY_ID, legalIds)).thenReturn(legalIdToPartyIdMap);
		when(mailboxStatusServiceMock.checkMailboxStatus(anyString(), anyList()))
			.thenThrow(Problem.valueOf(BAD_GATEWAY, "Failed to check mailbox status"));

		assertThatThrownBy(() -> precheckService.precheckLegalIds(MUNICIPALITY_ID, legalIds))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Bad Gateway: Failed to check mailbox status");

		verify(partyIntegrationMock).getPartyIds(MUNICIPALITY_ID, legalIds);
		verify(mailboxStatusServiceMock).checkMailboxStatus(anyString(), anyList());
		verifyNoMoreInteractions(partyIntegrationMock, mailboxStatusServiceMock);
	}

	@Test
	void precheckCSV(@Load(value = "/testfile/legalIds.csv") final String csv) throws IOException {
		final var multipartFileMock = Mockito.mock(MultipartFile.class);
		when(multipartFileMock.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

		// Mock partyIntegration - one entry has no partyId
		final var partyIdMap = new HashMap<String, String>();
		partyIdMap.put("201901012391", UUID.randomUUID().toString());
		partyIdMap.put("201901022382", UUID.randomUUID().toString());
		partyIdMap.put("201901032399", UUID.randomUUID().toString());
		partyIdMap.put("201901042380", UUID.randomUUID().toString());
		partyIdMap.put("201901052397", UUID.randomUUID().toString());
		partyIdMap.put("201901062388", null); // No partyId for this entry

		when(partyIntegrationMock.getPartyIds(eq(MUNICIPALITY_ID), anyList())).thenReturn(partyIdMap);

		final var result = precheckService.precheckCSV(MUNICIPALITY_ID, multipartFileMock);

		assertThat(result.duplicateEntries()).isEmpty();
		assertThat(result.rejectedEntries()).hasSize(1).contains("201901062388");

		verify(partyIntegrationMock).getPartyIds(eq(MUNICIPALITY_ID), anyList());
	}

	@Test
	void precheckCSV_withDuplicates(@Load(value = "/testfile/legalIds-duplicates.csv") final String csv) throws IOException {
		final var multipartFileMock = Mockito.mock(MultipartFile.class);
		when(multipartFileMock.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

		// Mock partyIntegration - all entries have partyIds
		final var partyIdMap = Map.of(
			"201901012391", UUID.randomUUID().toString(),
			"201901012392", UUID.randomUUID().toString());

		when(partyIntegrationMock.getPartyIds(eq(MUNICIPALITY_ID), anyList())).thenReturn(partyIdMap);

		final var result = precheckService.precheckCSV(MUNICIPALITY_ID, multipartFileMock);

		assertThat(result.duplicateEntries()).hasSize(2)
			.containsEntry("201901012391", 2)
			.containsEntry("201901012392", 2);
		assertThat(result.rejectedEntries()).isEmpty();

		verify(partyIntegrationMock).getPartyIds(eq(MUNICIPALITY_ID), anyList());
	}

	@Test
	void precheckCSV_withInvalidFormat(@Load(value = "/testfile/legalIds-invalid-format.csv") final String csv) throws IOException {
		final var multipartFileMock = Mockito.mock(MultipartFile.class);
		when(multipartFileMock.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

		assertThatThrownBy(() -> precheckService.precheckCSV(MUNICIPALITY_ID, multipartFileMock))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Bad Request: Invalid CSV format. Each data row must contain 12 digits, an optional hyphen between digit 8 and 9 are acceptable. Invalid entry: 20190--1012391");
	}

	@Test
	void precheckCSV_allEntriesWithoutPartyId(@Load(value = "/testfile/legalIds.csv") final String csv) throws IOException {
		final var multipartFileMock = Mockito.mock(MultipartFile.class);
		when(multipartFileMock.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

		// Mock partyIntegration to throw exception when no partyIds can be found
		when(partyIntegrationMock.getPartyIds(eq(MUNICIPALITY_ID), anyList()))
			.thenThrow(Problem.valueOf(BAD_GATEWAY, "Failed to retrieve party IDs"));

		assertThatThrownBy(() -> precheckService.precheckCSV(MUNICIPALITY_ID, multipartFileMock))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Bad Gateway: Failed to retrieve party IDs");

		verify(partyIntegrationMock).getPartyIds(eq(MUNICIPALITY_ID), anyList());
	}

	@Test
	void precheckCSV_noValidPartyIds(@Load(value = "/testfile/legalIds.csv") final String csv) throws IOException {
		final var multipartFileMock = Mockito.mock(MultipartFile.class);
		when(multipartFileMock.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

		// Mock partyIntegration - all entries have null partyId
		final var partyIdMap = new HashMap<String, String>();

		when(partyIntegrationMock.getPartyIds(eq(MUNICIPALITY_ID), anyList())).thenReturn(partyIdMap);

		assertThatThrownBy(() -> precheckService.precheckCSV(MUNICIPALITY_ID, multipartFileMock))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("No valid partyIds found");

		verify(partyIntegrationMock).getPartyIds(eq(MUNICIPALITY_ID), anyList());
	}

}
