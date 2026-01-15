package se.sundsvall.postportalservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.BAD_GATEWAY;
import static se.sundsvall.postportalservice.TestDataFactory.generateLegalId;

import generated.se.sundsvall.citizen.CitizenAddress;
import generated.se.sundsvall.citizen.CitizenExtended;
import generated.se.sundsvall.citizen.PersonGuidBatch;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
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

	@Mock(answer = Answers.CALLS_REAL_METHODS)
	private EntityMapper entityMapperMock;

	@InjectMocks
	private PrecheckService precheckService;

	@AfterEach
	void noMoreInteractions() {
		verifyNoMoreInteractions(citizenIntegrationMock, entityMapperMock, mailboxStatusServiceMock,
			digitalRegisteredLetterIntegrationMock);
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

		final var citizens = List.of(snailMailCitizen, notEligibleCitizen);

		when(citizenIntegrationMock.getCitizens(MUNICIPALITY_ID, List.of(snailMailUuid, notEligibleUuid))).thenReturn(citizens);

		// Mock person numbers for age verification
		final var digitalMailBatch = new PersonGuidBatch()
			.success(Boolean.TRUE)
			.personId(UUID.fromString(digitalMailUuid))
			.personNumber(generateLegalId(30, "2399"));

		final var snailMailBatch = new PersonGuidBatch()
			.success(Boolean.TRUE)
			.personId(UUID.fromString(snailMailUuid))
			.personNumber(generateLegalId(35, "2398"));

		final var notEligibleBatch = new PersonGuidBatch()
			.success(Boolean.TRUE)
			.personId(UUID.fromString(notEligibleUuid))
			.personNumber(generateLegalId(40, "2388"));

		when(citizenIntegrationMock.getPersonNumbers(MUNICIPALITY_ID, partyIds))
			.thenReturn(List.of(digitalMailBatch, snailMailBatch, notEligibleBatch));

		final var result = precheckService.precheckPartyIds(MUNICIPALITY_ID, partyIds);

		assertThat(result.precheckRecipients()).hasSize(3)
			.extracting(PrecheckRecipient::partyId, PrecheckRecipient::deliveryMethod)
			.containsExactlyInAnyOrder(
				tuple(digitalMailUuid, DeliveryMethod.DIGITAL_MAIL),
				tuple(snailMailUuid, DeliveryMethod.SNAIL_MAIL),
				tuple(notEligibleUuid, DeliveryMethod.DELIVERY_NOT_POSSIBLE));

		verify(mailboxStatusServiceMock).checkMailboxStatus(MUNICIPALITY_ID, partyIds);
		verify(citizenIntegrationMock).getCitizens(MUNICIPALITY_ID, List.of(snailMailUuid, notEligibleUuid));
		verify(citizenIntegrationMock).getPersonNumbers(MUNICIPALITY_ID, partyIds);
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
	void precheckLegalIds_adultWithDigitalMailbox() {
		final var legalId = generateLegalId(35, "0000");
		final var partyId = UUID.randomUUID().toString();

		final var personGuidBatch = new PersonGuidBatch()
			.success(Boolean.TRUE)
			.personNumber(legalId)
			.personId(UUID.fromString(partyId));

		final var legalIds = List.of(legalId);
		final var personGuidBatches = List.of(personGuidBatch);
		final var partyIds = List.of(partyId);

		when(citizenIntegrationMock.getPartyIds(MUNICIPALITY_ID, legalIds)).thenReturn(personGuidBatches);

		// Mock mailbox status - digital mailbox available
		final var mailboxStatus = new MailboxStatus(List.of(partyId), List.of());
		when(mailboxStatusServiceMock.checkMailboxStatus(MUNICIPALITY_ID, partyIds)).thenReturn(mailboxStatus);

		final var result = precheckService.precheckLegalIds(MUNICIPALITY_ID, legalIds);

		assertThat(result).hasSize(1)
			.extracting(RecipientEntity::getPartyId, RecipientEntity::getMessageType, RecipientEntity::getStatus)
			.containsExactly(tuple(partyId, MessageType.DIGITAL_MAIL, "PENDING"));

		// Verify
		verify(citizenIntegrationMock).getPartyIds(MUNICIPALITY_ID, legalIds);
		verify(mailboxStatusServiceMock).checkMailboxStatus(MUNICIPALITY_ID, partyIds);
		verify(entityMapperMock).toDigitalMailRecipientEntity(any());
	}

	@Test
	void precheckLegalIds_adultWithoutMailboxRegistered() {
		final var legalId = generateLegalId(40, "2390");
		final var partyId = UUID.randomUUID().toString();

		final var personGuidBatch = new PersonGuidBatch()
			.success(Boolean.TRUE)
			.personNumber(legalId)
			.personId(UUID.fromString(partyId));

		final var legalIds = List.of(legalId);
		final var personGuidBatches = List.of(personGuidBatch);
		final var partyIds = List.of(partyId);

		when(citizenIntegrationMock.getPartyIds(MUNICIPALITY_ID, legalIds)).thenReturn(personGuidBatches);

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

		verify(citizenIntegrationMock).getPartyIds(MUNICIPALITY_ID, legalIds);
		verify(mailboxStatusServiceMock).checkMailboxStatus(MUNICIPALITY_ID, partyIds);
		verify(citizenIntegrationMock).getCitizens(MUNICIPALITY_ID, partyIds);
		verify(entityMapperMock).toSnailMailRecipientEntity(any());
	}

	@Test
	void precheckLegalIds_notRegisteredInSweden() {
		final var legalId = generateLegalId(45, "2381");
		final var partyId = UUID.randomUUID().toString();

		final var personGuidBatch = new PersonGuidBatch()
			.success(Boolean.TRUE)
			.personNumber(legalId)
			.personId(UUID.fromString(partyId));

		final var legalIds = List.of(legalId);
		final var personGuidBatches = List.of(personGuidBatch);
		final var partyIds = List.of(partyId);

		when(citizenIntegrationMock.getPartyIds(MUNICIPALITY_ID, legalIds)).thenReturn(personGuidBatches);

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
		verify(citizenIntegrationMock).getPartyIds(MUNICIPALITY_ID, legalIds);
		verify(mailboxStatusServiceMock).checkMailboxStatus(MUNICIPALITY_ID, partyIds);
		verify(citizenIntegrationMock).getCitizens(MUNICIPALITY_ID, partyIds);
		verify(entityMapperMock).toUndeliverableRecipientEntity(any());
	}

	@Test
	void precheckLegalIds_minorIneligible() {
		// Generate legal ID for 15-year-old minor
		final var legalId = generateLegalId(15, "2387");
		final var partyId = UUID.randomUUID().toString();

		final var personGuidBatch = new PersonGuidBatch()
			.success(Boolean.TRUE)
			.personNumber(legalId)
			.personId(UUID.fromString(partyId));

		final var legalIds = List.of(legalId);
		final var personGuidBatches = List.of(personGuidBatch);
		final var partyIds = List.of(partyId);

		// Setup mocks
		when(citizenIntegrationMock.getPartyIds(MUNICIPALITY_ID, legalIds)).thenReturn(personGuidBatches);

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
		verify(citizenIntegrationMock).getPartyIds(MUNICIPALITY_ID, legalIds);
		verify(mailboxStatusServiceMock).checkMailboxStatus(MUNICIPALITY_ID, partyIds);
		verify(citizenIntegrationMock).getCitizens(MUNICIPALITY_ID, partyIds);
		verify(entityMapperMock).toIneligibleMinorRecipientEntity(any());
	}

	@Test
	void precheckLegalIds_citizenThrows() {
		final var legalIds = List.of("201801022383", "201801032390", "201801042381");
		when(citizenIntegrationMock.getPartyIds(MUNICIPALITY_ID, legalIds)).thenThrow(Problem.valueOf(BAD_GATEWAY, "Failed to retrieve citizen data"));

		assertThatThrownBy(() -> precheckService.precheckLegalIds(MUNICIPALITY_ID, legalIds))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Bad Gateway: Failed to retrieve citizen data");

		verify(citizenIntegrationMock).getPartyIds(MUNICIPALITY_ID, legalIds);
		verifyNoInteractions(mailboxStatusServiceMock);
	}

	@Test
	void precheckLegalIds_mailboxServiceThrows() {
		final var legalIds = List.of("201801022383", "201801032390", "201801042381");
		final var batches = List.of(new PersonGuidBatch()
			.success(Boolean.TRUE)
			.personNumber(legalIds.getFirst())
			.personId(UUID.randomUUID()));

		when(citizenIntegrationMock.getPartyIds(MUNICIPALITY_ID, legalIds)).thenReturn(batches);
		when(mailboxStatusServiceMock.checkMailboxStatus(anyString(), anyList()))
			.thenThrow(Problem.valueOf(BAD_GATEWAY, "Failed to check mailbox status"));

		assertThatThrownBy(() -> precheckService.precheckLegalIds(MUNICIPALITY_ID, legalIds))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Bad Gateway: Failed to check mailbox status");

		verify(citizenIntegrationMock).getPartyIds(MUNICIPALITY_ID, legalIds);
		verify(mailboxStatusServiceMock).checkMailboxStatus(anyString(), anyList());
		verifyNoMoreInteractions(citizenIntegrationMock, mailboxStatusServiceMock);
	}

	@Test
	void precheckCSV(@Load(value = "/testfile/legalIds.csv") final String csv) throws IOException {
		var multipartFileMock = Mockito.mock(MultipartFile.class);
		when(multipartFileMock.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

		var result = precheckService.precheckCSV(multipartFileMock);

		assertThat(result.duplicateEntries()).isEmpty();
	}

	@Test
	void precheckCSV_withDuplicates(@Load(value = "/testfile/legalIds-duplicates.csv") final String csv) throws IOException {
		var multipartFileMock = Mockito.mock(MultipartFile.class);
		when(multipartFileMock.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

		var result = precheckService.precheckCSV(multipartFileMock);

		assertThat(result.duplicateEntries()).hasSize(2)
			.containsEntry("201901012391", 2)
			.containsEntry("201901012392", 2);
	}

	@Test
	void precheckCSV_withInvalidFormat(@Load(value = "/testfile/legalIds-invalid-format.csv") final String csv) throws IOException {
		var multipartFileMock = Mockito.mock(MultipartFile.class);
		when(multipartFileMock.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

		assertThatThrownBy(() -> precheckService.precheckCSV(multipartFileMock))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Bad Request: Invalid CSV format. Each data row must contain 12 digits, an optional hyphen between digit 8 and 9 are acceptable. Invalid entry: 20190--1012391");
	}

}
