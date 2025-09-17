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
	void getPopulationRegistrationAddress_emptyIfNoCitizen() {
		final var result = citizenIntegration.getPopulationRegistrationAddress(null);

		assertThat(result).isEmpty();
	}

	@Test
	void getPopulationRegistrationAddress_emptyIfNoAddresses() {
		final var citizen = createCitizen(emptyList());

		final var result = citizenIntegration.getPopulationRegistrationAddress(citizen);

		assertThat(result).isEmpty();
	}

	@Test
	void getPopulationRegistrationAddress_emptyIfNoMatchingType() {
		final var citizen = createCitizen(List.of(createAddress("SOME_ADDRESS_TYPE"), createAddress("FOREIGN_ADDRESS")));

		final var result = citizenIntegration.getPopulationRegistrationAddress(citizen);

		assertThat(result).isEmpty();
	}

	@Test
	void getPopulationRegistrationAddress_firstMatching() {
		final var address1 = createAddress("SOME_ADDRESS_TYPE");
		final var address2 = createAddress(CitizenIntegration.POPULATION_REGISTRATION_ADDRESS);
		final var address3 = createAddress(CitizenIntegration.POPULATION_REGISTRATION_ADDRESS);
		final var citizen = createCitizen(List.of(address1, address2, address3));

		final var result = citizenIntegration.getPopulationRegistrationAddress(citizen);

		assertThat(result).isPresent();
		assertThat(result.get()).isEqualTo(address2);
	}

	private CitizenAddress createAddress(String type) {
		var address = new CitizenAddress();
		address.setAddressType(type);
		return address;
	}

	private CitizenExtended createCitizen(List<CitizenAddress> addresses) {
		var citizen = new CitizenExtended();
		citizen.setAddresses(addresses);
		return citizen;
	}
}
