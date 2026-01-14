package se.sundsvall.postportalservice.integration.citizen;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import generated.se.sundsvall.citizen.CitizenAddress;
import generated.se.sundsvall.citizen.CitizenExtended;
import generated.se.sundsvall.citizen.PersonGuidBatch;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CitizenIntegrationTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final List<String> PARTY_IDS = List.of(
		"28fba79e-73aa-4ecb-939f-301f326d2d4c",
		"f560865a-51f0-4e96-bca1-55d57a0d3f68");
	private static final List<String> PERSON_IDS = List.of(
		"191111-1111",
		"192222-2222");

	@Mock
	private CitizenClient citizenClientMock;

	@InjectMocks
	private CitizenIntegration citizenIntegration;

	@AfterEach
	void verifyInteractions() {
		verifyNoMoreInteractions(citizenClientMock);
	}

	@Test
	void getCitizens() {
		final var citizen1 = createCitizen(emptyList());
		final var citizen2 = createCitizen(emptyList());
		final var citizens = List.of(citizen1, citizen2);

		when(citizenClientMock.getCitizens(MUNICIPALITY_ID, PARTY_IDS)).thenReturn(citizens);

		final var result = citizenIntegration.getCitizens(MUNICIPALITY_ID, PARTY_IDS);

		assertThat(result).hasSize(2);
		assertThat(result).containsExactly(citizen1, citizen2);
		verify(citizenClientMock).getCitizens(MUNICIPALITY_ID, PARTY_IDS);
	}

	@Test
	void getCitizens_shouldReturnEmptyList_whenPartyIdsAreNull() {
		final var result = citizenIntegration.getCitizens(MUNICIPALITY_ID, null);

		assertThat(result).isNotNull().isEmpty();
	}

	@Test
	void getPartyIds() {
		final var personGuidBatch1 = new PersonGuidBatch();
		final var personGuidBatch2 = new PersonGuidBatch();

		personGuidBatch1.setPersonId(UUID.fromString("28fba79e-73aa-4ecb-939f-301f326d2d4c"));
		personGuidBatch2.setPersonId(UUID.fromString("f560865a-51f0-4e96-bca1-55d57a0d3f68"));

		final var personGuidBatches = List.of(
			personGuidBatch1,
			personGuidBatch2);

		when(citizenClientMock.getPartyIds(MUNICIPALITY_ID, PERSON_IDS)).thenReturn(personGuidBatches);

		final var result = citizenIntegration.getPartyIds(MUNICIPALITY_ID, PERSON_IDS);

		assertThat(result).hasSize(2);
		assertThat(result).isEqualTo(personGuidBatches);
		verify(citizenClientMock).getPartyIds(MUNICIPALITY_ID, PERSON_IDS);
	}

	@Test
	void getPartyIds_shouldReturnEmptyList_whenPersonIdsAreNull() {
		final var result = citizenIntegration.getPartyIds(MUNICIPALITY_ID, null);

		assertThat(result).isNotNull().isEmpty();
	}

	@Test
	void getPersonNumbers() {
		final var personGuidBatch1 = new PersonGuidBatch();
		final var personGuidBatch2 = new PersonGuidBatch();

		personGuidBatch1.setPersonId(UUID.fromString("28fba79e-73aa-4ecb-939f-301f326d2d4c"));
		personGuidBatch2.setPersonId(UUID.fromString("f560865a-51f0-4e96-bca1-55d57a0d3f68"));

		final var personGuidBatches = List.of(
			personGuidBatch1,
			personGuidBatch2);

		when(citizenClientMock.getLegalIds(MUNICIPALITY_ID, PARTY_IDS)).thenReturn(personGuidBatches);

		final var result = citizenIntegration.getPersonNumbers(MUNICIPALITY_ID, PARTY_IDS);

		assertThat(result).hasSize(2);
		assertThat(result).isEqualTo(personGuidBatches);
		verify(citizenClientMock).getLegalIds(MUNICIPALITY_ID, PARTY_IDS);
	}

	@Test
	void getPersonNumbers_shouldReturnEmptyList_whenPartyIdsAreNull() {
		final var result = citizenIntegration.getPersonNumbers(MUNICIPALITY_ID, null);

		assertThat(result).isNotNull().isEmpty();
	}

	private CitizenExtended createCitizen(List<CitizenAddress> addresses) {
		var citizen = new CitizenExtended();
		citizen.setAddresses(addresses);
		return citizen;
	}
}
