package se.sundsvall.postportalservice.service;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.postportalservice.service.PrecheckService.FAILURE_REASON_NO_ELIGIBLE_DELIVERY_METHOD;
import static se.sundsvall.postportalservice.service.PrecheckService.FAILURE_REASON_UNKNOWN_ERROR;

import com.fasterxml.jackson.databind.ObjectMapper;
import generated.se.sundsvall.citizen.PersonGuidBatch;
import generated.se.sundsvall.messaging.Mailbox;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.Problem;
import se.sundsvall.postportalservice.api.model.PrecheckResponse.DeliveryMethod;
import se.sundsvall.postportalservice.api.model.PrecheckResponse.PrecheckRecipient;
import se.sundsvall.postportalservice.integration.citizen.CitizenIntegration;
import se.sundsvall.postportalservice.integration.messaging.MessagingIntegration;
import se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration;
import se.sundsvall.postportalservice.service.mapper.PrecheckMapper;

@ExtendWith(MockitoExtension.class)
class PrecheckServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String DEPARTMENT_ID = "dept44";
	private static final String ORGANIZATION_NUMBER = "5555555555";

	@Mock
	CitizenIntegration citizenIntegrationMock;

	@Mock
	MessagingSettingsIntegration messagingSettingsIntegrationMock;

	@Mock
	MessagingIntegration messagingIntegrationMock;

	@Mock
	PrecheckMapper precheckMapper;

	@InjectMocks
	PrecheckService precheckService;

	@AfterEach
	void noMoreInteractions() {
		verifyNoMoreInteractions(citizenIntegrationMock);
		verifyNoMoreInteractions(messagingSettingsIntegrationMock);
		verifyNoMoreInteractions(messagingIntegrationMock);
	}

	@Test
	void precheck_withNoEntries() {
		final var result = precheckService.precheck(MUNICIPALITY_ID, DEPARTMENT_ID, emptyList());

		assertThat(result.precheckRecipients()).isEmpty();
	}

	@Test
	void precheck_withAllReachableDigitalMailboxes() {
		final var personId1 = "191111111111";
		final var personId2 = "192222222222";
		final var personIds = List.of(personId1, personId2);

		final var batches = okBatches(personId1, personId2);
		final var partyIds = batches.stream()
			.map(batch -> requireNonNull(batch.getPersonId()).toString())
			.toList();

		final var isReachable = true;

		when(precheckMapper.toFailureByPersonId(anyList())).thenReturn(Map.of());
		when(precheckMapper.mapPersonIdToPartyId(anyList())).thenReturn(Map.of(
			personId1, partyIds.get(0),
			personId2, partyIds.get(1)));
		when(precheckMapper.toSnailMailEligiblePartyIds(anyList(), any())).thenReturn(emptyList());
		when(citizenIntegrationMock.getPartyIds(MUNICIPALITY_ID, personIds)).thenReturn(batches);
		when(messagingSettingsIntegrationMock.getOrganizationNumber(MUNICIPALITY_ID, DEPARTMENT_ID)).thenReturn(Optional.of(ORGANIZATION_NUMBER));
		when(messagingIntegrationMock.precheckMailboxes(eq(MUNICIPALITY_ID), eq(ORGANIZATION_NUMBER), anyList())).thenReturn(List.of(
			createMailbox(partyIds.get(0), isReachable),
			createMailbox(partyIds.get(1), isReachable)));
		when(citizenIntegrationMock.getCitizens(eq(MUNICIPALITY_ID), anyList())).thenReturn(emptyList());

		final var result = precheckService.precheck(MUNICIPALITY_ID, DEPARTMENT_ID, personIds);

		assertThat(result.precheckRecipients()).hasSize(2);
		assertThat(result.precheckRecipients().get(0))
			.extracting(
				PrecheckRecipient::personalIdentityNumber,
				PrecheckRecipient::deliveryMethod,
				PrecheckRecipient::partyId,
				PrecheckRecipient::reason)
			.containsExactly(personId1, DeliveryMethod.DIGITAL_MAIL, partyIds.get(0), null);

		assertThat(result.precheckRecipients().get(1))
			.extracting(
				PrecheckRecipient::personalIdentityNumber,
				PrecheckRecipient::deliveryMethod,
				PrecheckRecipient::partyId,
				PrecheckRecipient::reason)
			.containsExactly(personId2, DeliveryMethod.DIGITAL_MAIL, partyIds.get(1), null);

		verify(citizenIntegrationMock).getPartyIds(MUNICIPALITY_ID, personIds);
		verify(messagingSettingsIntegrationMock).getOrganizationNumber(MUNICIPALITY_ID, DEPARTMENT_ID);
		verify(messagingIntegrationMock).precheckMailboxes(
			eq(MUNICIPALITY_ID),
			eq(ORGANIZATION_NUMBER),
			argThat(ids -> ids.size() == partyIds.size() && ids.containsAll(partyIds) && partyIds.containsAll(ids)));
		verify(citizenIntegrationMock).getCitizens(eq(MUNICIPALITY_ID), anyList());
	}

	@Test
	void precheck_withFailureRow() {
		final var personIdOk = "191111111111";
		final var personIdFail = "193333333333";
		final var personIds = List.of(personIdOk, personIdFail);

		final var ok = createPersonGuidBatch(personIdOk, true, FAILURE_REASON_NO_ELIGIBLE_DELIVERY_METHOD);
		final var fail = createPersonGuidBatch(personIdFail, false, "Some random error");

		when(precheckMapper.toFailureByPersonId(anyList()))
			.thenReturn(Map.of(personIdFail, "Some random error"));

		final var idMap = new HashMap<String, String>();

		idMap.put(personIdOk, requireNonNull(ok.getPersonId()).toString());
		idMap.put(personIdFail, null);

		when(precheckMapper.mapPersonIdToPartyId(anyList())).thenReturn(idMap);
		when(precheckMapper.toSnailMailEligiblePartyIds(anyList(), any()))
			.thenReturn(emptyList());
		when(citizenIntegrationMock.getPartyIds(MUNICIPALITY_ID, personIds)).thenReturn(List.of(ok, fail));

		final var org = Optional.of(ORGANIZATION_NUMBER);

		when(messagingSettingsIntegrationMock.getOrganizationNumber(MUNICIPALITY_ID, DEPARTMENT_ID)).thenReturn(org);

		final var okPartyId = requireNonNull(ok.getPersonId()).toString();

		when(messagingIntegrationMock.precheckMailboxes(eq(MUNICIPALITY_ID), eq(ORGANIZATION_NUMBER), anyList()))
			.thenReturn(List.of(createMailbox(okPartyId, false)));
		when(citizenIntegrationMock.getCitizens(eq(MUNICIPALITY_ID), anyList())).thenReturn(emptyList());

		final var result = precheckService.precheck(MUNICIPALITY_ID, DEPARTMENT_ID, personIds);

		assertThat(result.precheckRecipients()).extracting(
			PrecheckRecipient::personalIdentityNumber,
			PrecheckRecipient::deliveryMethod,
			PrecheckRecipient::partyId,
			PrecheckRecipient::reason)
			.containsExactly(
				tuple(personIdOk, DeliveryMethod.DELIVERY_NOT_POSSIBLE, okPartyId, FAILURE_REASON_NO_ELIGIBLE_DELIVERY_METHOD),
				tuple(personIdFail, DeliveryMethod.DELIVERY_NOT_POSSIBLE, null, "Some random error"));

		verify(citizenIntegrationMock).getPartyIds(MUNICIPALITY_ID, personIds);
		verify(messagingSettingsIntegrationMock).getOrganizationNumber(MUNICIPALITY_ID, DEPARTMENT_ID);
		verify(messagingIntegrationMock).precheckMailboxes(MUNICIPALITY_ID, ORGANIZATION_NUMBER, List.of(okPartyId));
		verify(citizenIntegrationMock).getCitizens(eq(MUNICIPALITY_ID), anyList());
	}

	@Test
	void precheck_withFailureRowWhenErrorIsMissing() throws Exception {
		final var personIdOk = "191111111111";
		final var personIdFail = "193333333333";
		final var personIds = List.of(personIdOk, personIdFail);

		final var ok = createPersonGuidBatch(personIdOk, true, null);
		final var fail = createPersonGuidBatch(personIdFail, false, null);

		when(precheckMapper.toFailureByPersonId(anyList()))
			.thenReturn(Map.of(personIdFail, FAILURE_REASON_UNKNOWN_ERROR));

		final var idMap = new HashMap<String, String>();

		idMap.put(personIdOk, requireNonNull(ok.getPersonId()).toString());
		idMap.put(personIdFail, null);

		when(precheckMapper.mapPersonIdToPartyId(anyList())).thenReturn(idMap);
		when(precheckMapper.toSnailMailEligiblePartyIds(anyList(), any()))
			.thenReturn(java.util.Collections.emptyList());
		when(citizenIntegrationMock.getPartyIds(MUNICIPALITY_ID, personIds)).thenReturn(List.of(ok, fail));
		when(messagingSettingsIntegrationMock.getOrganizationNumber(MUNICIPALITY_ID, DEPARTMENT_ID))
			.thenReturn(Optional.of(ORGANIZATION_NUMBER));

		final var okPartyId = requireNonNull(ok.getPersonId()).toString();

		when(messagingIntegrationMock.precheckMailboxes(eq(MUNICIPALITY_ID), eq(ORGANIZATION_NUMBER), anyList()))
			.thenReturn(List.of(createMailbox(okPartyId, false)));
		when(citizenIntegrationMock.getCitizens(eq(MUNICIPALITY_ID), anyList())).thenReturn(emptyList());

		final var result = precheckService.precheck(MUNICIPALITY_ID, DEPARTMENT_ID, personIds);

		assertThat(result.precheckRecipients()).extracting(
			PrecheckRecipient::personalIdentityNumber,
			PrecheckRecipient::deliveryMethod,
			PrecheckRecipient::partyId,
			PrecheckRecipient::reason)
			.containsExactly(
				tuple(personIdOk, DeliveryMethod.DELIVERY_NOT_POSSIBLE, okPartyId, FAILURE_REASON_NO_ELIGIBLE_DELIVERY_METHOD),
				tuple(personIdFail, DeliveryMethod.DELIVERY_NOT_POSSIBLE, null, FAILURE_REASON_UNKNOWN_ERROR));

		final var mapper = new ObjectMapper();
		final var json = mapper.writeValueAsString(result);
		final var root = mapper.readTree(json);

		assertThat(root.at("/recipients/1/reason").asText()).isEqualTo(FAILURE_REASON_UNKNOWN_ERROR);
		assertThat(root.at("/recipients/0/partyId").asText()).isEqualTo(okPartyId);

		verify(citizenIntegrationMock).getPartyIds(MUNICIPALITY_ID, personIds);
		verify(messagingSettingsIntegrationMock).getOrganizationNumber(MUNICIPALITY_ID, DEPARTMENT_ID);
		verify(messagingIntegrationMock).precheckMailboxes(MUNICIPALITY_ID, ORGANIZATION_NUMBER, List.of(okPartyId));
		verify(citizenIntegrationMock).getCitizens(eq(MUNICIPALITY_ID), anyList());
	}

	@Test
	void precheck_throwsWenNoOrganizationNumber() {
		final var personId = "191111111111";

		when(precheckMapper.toFailureByPersonId(anyList())).thenReturn(Map.of());
		when(precheckMapper.mapPersonIdToPartyId(anyList())).thenReturn(Map.of());
		when(citizenIntegrationMock.getPartyIds(MUNICIPALITY_ID, List.of(personId))).thenReturn(okBatches(personId));
		when(messagingSettingsIntegrationMock.getOrganizationNumber(MUNICIPALITY_ID, DEPARTMENT_ID)).thenReturn(Optional.empty());
		when(precheckMapper.mapPersonIdToPartyId(anyList()))
			.thenReturn(Map.of(
				personId, UUID.randomUUID().toString()));

		assertThatThrownBy(() -> precheckService.precheck(MUNICIPALITY_ID, DEPARTMENT_ID, List.of(personId)))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Organization number not found");

		verify(citizenIntegrationMock).getPartyIds(MUNICIPALITY_ID, List.of(personId));
		verify(messagingSettingsIntegrationMock).getOrganizationNumber(MUNICIPALITY_ID, DEPARTMENT_ID);
	}

	@Test
	void precheck_withOnlyFailures() {
		final var personId1 = "191111111111";
		final var personId2 = "192222222222";
		final var personIds = List.of(personId1, personId2);
		final var failures = Map.of(
			personId1, "not found",
			personId2, "timeout");

		when(precheckMapper.toFailureByPersonId(anyList())).thenReturn(failures);
		when(precheckMapper.toRecipientsWithoutPartyIds(personIds, failures)).thenReturn(List.of(
			new PrecheckRecipient(personId1, null, DeliveryMethod.DELIVERY_NOT_POSSIBLE, "not found"),
			new PrecheckRecipient(personId2, null, DeliveryMethod.DELIVERY_NOT_POSSIBLE, "timeout")));
		when(citizenIntegrationMock.getPartyIds(MUNICIPALITY_ID, List.of(personId1, personId2))).thenReturn(List.of(
			createPersonGuidBatch(personId1, false, "not found"),
			createPersonGuidBatch(personId2, false, "timeout")));

		final var result = precheckService.precheck(MUNICIPALITY_ID, DEPARTMENT_ID, personIds);

		assertThat(result.precheckRecipients()).extracting(
			PrecheckRecipient::personalIdentityNumber,
			PrecheckRecipient::deliveryMethod,
			PrecheckRecipient::partyId,
			PrecheckRecipient::reason)
			.containsExactly(
				tuple(personId1, DeliveryMethod.DELIVERY_NOT_POSSIBLE, null, "not found"),
				tuple(personId2, DeliveryMethod.DELIVERY_NOT_POSSIBLE, null, "timeout"));

		verify(citizenIntegrationMock).getPartyIds(MUNICIPALITY_ID, List.of(personId1, personId2));
	}

	private static List<PersonGuidBatch> okBatches(String... personIds) {
		return Arrays.stream(personIds)
			.map(p -> createPersonGuidBatch(p, true, null))
			.toList();
	}

	private static PersonGuidBatch createPersonGuidBatch(String personId, boolean success, String errorMessage) {
		final var personGuidBatch = new PersonGuidBatch();

		personGuidBatch.setPersonNumber(personId);

		if (success) {
			personGuidBatch.setSuccess(true);
			personGuidBatch.setErrorMessage(null);
			personGuidBatch.setPersonId(UUID.randomUUID());
		} else {
			personGuidBatch.setSuccess(false);
			personGuidBatch.setErrorMessage(errorMessage == null ? FAILURE_REASON_UNKNOWN_ERROR : errorMessage);
			personGuidBatch.setPersonId(null);
		}

		return personGuidBatch;
	}

	private static Mailbox createMailbox(String partyId, Boolean isReachable) {
		final var mailbox = new Mailbox();

		mailbox.setPartyId(partyId);
		mailbox.setReachable(isReachable);

		return mailbox;
	}
}
