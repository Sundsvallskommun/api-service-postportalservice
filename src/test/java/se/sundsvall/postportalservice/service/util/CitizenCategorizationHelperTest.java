package se.sundsvall.postportalservice.service.util;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.postportalservice.TestDataFactory.generateLegalId;

import generated.se.sundsvall.citizen.CitizenExtended;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CitizenCategorizationHelperTest {

	private static final String ADULT_LEGAL_ID = generateLegalId(30, "0000"); // Adult
	private static final String ADULT_LEGAL_ID2 = generateLegalId(29, "0000"); // Adult #2
	private static final String MINOR_LEGAL_ID = generateLegalId(15, "0000"); // Minor
	private static final String MINOR_LEGAL_ID2 = generateLegalId(13, "0000"); // Minor #2

	@Test
	void isAdult_withAdultCitizen() {
		final var partyId = UUID.randomUUID().toString();
		final var mapping = Map.of(partyId, ADULT_LEGAL_ID); // Adult

		final var citizen = new CitizenExtended().personId(UUID.fromString(partyId));

		final var predicate = CitizenCategorizationHelper.isAdult(mapping);

		assertThat(predicate.test(citizen)).isTrue();
	}

	@Test
	void isAdult_withMinorCitizen() {
		final var partyId = UUID.randomUUID().toString();
		final var mapping = Map.of(partyId, MINOR_LEGAL_ID); // Minor

		final var citizen = new CitizenExtended()
			.personId(UUID.fromString(partyId));

		final var predicate = CitizenCategorizationHelper.isAdult(mapping);

		assertThat(predicate.test(citizen)).isFalse();
	}

	@Test
	void isAdult_withNullPartyId() {
		final var mapping = Map.of(UUID.randomUUID().toString(), ADULT_LEGAL_ID);

		final var citizen = new CitizenExtended()
			.personId(null);

		final var predicate = CitizenCategorizationHelper.isAdult(mapping);

		assertThat(predicate.test(citizen)).isFalse();
	}

	@Test
	void isAdult_withPartyIdNotInMapping() {
		final var mapping = Map.of(UUID.randomUUID().toString(), ADULT_LEGAL_ID);

		final var citizen = new CitizenExtended()
			.personId(UUID.randomUUID());

		final var predicate = CitizenCategorizationHelper.isAdult(mapping);

		assertThat(predicate.test(citizen)).isFalse();
	}

	@Test
	void isMinor_withMinorCitizen() {
		final var partyId = UUID.randomUUID().toString();
		final var mapping = Map.of(partyId, MINOR_LEGAL_ID); // Born 2010, minor

		final var citizen = new CitizenExtended()
			.personId(UUID.fromString(partyId));

		final var predicate = CitizenCategorizationHelper.isMinor(mapping);

		assertThat(predicate.test(citizen)).isTrue();
	}

	@Test
	void isMinor_withAdultCitizen() {
		final var partyId = UUID.randomUUID().toString();
		final var mapping = Map.of(partyId, ADULT_LEGAL_ID); // Born 1990, adult

		final var citizen = new CitizenExtended()
			.personId(UUID.fromString(partyId));

		final var predicate = CitizenCategorizationHelper.isMinor(mapping);

		assertThat(predicate.test(citizen)).isFalse();
	}

	@Test
	void isMinor_withNullPartyId() {
		final var mapping = Map.of(UUID.randomUUID().toString(), MINOR_LEGAL_ID);

		final var citizen = new CitizenExtended()
			.personId(null);

		final var predicate = CitizenCategorizationHelper.isMinor(mapping);

		assertThat(predicate.test(citizen)).isFalse();
	}

	@Test
	void isMinor_withPartyIdNotInMapping() {
		final var mapping = Map.of(UUID.randomUUID().toString(), MINOR_LEGAL_ID);

		final var citizen = new CitizenExtended()
			.personId(UUID.randomUUID());

		final var predicate = CitizenCategorizationHelper.isMinor(mapping);

		assertThat(predicate.test(citizen)).isFalse();
	}

	@Test
	void categorizeCitizens_withMixedCitizens() {
		final var adultPartyId = UUID.randomUUID().toString();
		final var minorPartyId = UUID.randomUUID().toString();
		final var notInSwedenPartyId = UUID.randomUUID().toString();

		final var mapping = Map.of(
			adultPartyId, ADULT_LEGAL_ID, // Adult
			minorPartyId, MINOR_LEGAL_ID, // Minor
			notInSwedenPartyId, "198001011234" // Adult but not in Sweden
		);

		final var adultCitizen = new CitizenExtended()
			.personId(UUID.fromString(adultPartyId));

		final var minorCitizen = new CitizenExtended()
			.personId(UUID.fromString(minorPartyId));

		final var notInSwedenCitizen = new CitizenExtended()
			.personId(UUID.fromString(notInSwedenPartyId));

		final var result = CitizenCategorizationHelper.categorizeCitizens(
			List.of(adultCitizen, minorCitizen, notInSwedenCitizen),
			mapping,
			citizen -> !citizen.getPersonId().equals(UUID.fromString(notInSwedenPartyId)));

		assertThat(result.eligibleAdults()).hasSize(1)
			.extracting(CitizenExtended::getPersonId)
			.containsExactly(UUID.fromString(adultPartyId));

		assertThat(result.ineligibleMinors()).hasSize(1)
			.extracting(CitizenExtended::getPersonId)
			.containsExactly(UUID.fromString(minorPartyId));

		assertThat(result.notRegisteredInSweden()).hasSize(1)
			.extracting(CitizenExtended::getPersonId)
			.containsExactly(UUID.fromString(notInSwedenPartyId));
	}

	@Test
	void categorizeCitizens_withNullCitizens() {
		final var mapping = Map.of(UUID.randomUUID().toString(), ADULT_LEGAL_ID);

		final var result = CitizenCategorizationHelper.categorizeCitizens(
			null,
			mapping,
			citizen -> true);

		assertThat(result.eligibleAdults()).isEmpty();
		assertThat(result.ineligibleMinors()).isEmpty();
		assertThat(result.notRegisteredInSweden()).isEmpty();
	}

	@Test
	void categorizeCitizens_withEmptyList() {
		final var mapping = Map.of(UUID.randomUUID().toString(), ADULT_LEGAL_ID);

		final var result = CitizenCategorizationHelper.categorizeCitizens(
			List.of(),
			mapping,
			citizen -> true);

		assertThat(result.eligibleAdults()).isEmpty();
		assertThat(result.ineligibleMinors()).isEmpty();
		assertThat(result.notRegisteredInSweden()).isEmpty();
	}

	@Test
	void categorizeCitizens_withAllAdults() {
		final var partyId1 = UUID.randomUUID().toString();
		final var partyId2 = UUID.randomUUID().toString();

		final var mapping = Map.of(
			partyId1, ADULT_LEGAL_ID,
			partyId2, ADULT_LEGAL_ID2);

		final var citizen1 = new CitizenExtended()
			.personId(UUID.fromString(partyId1));

		final var citizen2 = new CitizenExtended()
			.personId(UUID.fromString(partyId2));

		final var result = CitizenCategorizationHelper.categorizeCitizens(
			List.of(citizen1, citizen2),
			mapping,
			citizen -> true);

		assertThat(result.eligibleAdults()).hasSize(2);
		assertThat(result.ineligibleMinors()).isEmpty();
		assertThat(result.notRegisteredInSweden()).isEmpty();
	}

	@Test
	void categorizeCitizens_withAllMinors() {
		final var partyId1 = UUID.randomUUID().toString();
		final var partyId2 = UUID.randomUUID().toString();

		final var mapping = Map.of(
			partyId1, MINOR_LEGAL_ID,
			partyId2, MINOR_LEGAL_ID2);

		final var citizen1 = new CitizenExtended()
			.personId(UUID.fromString(partyId1));

		final var citizen2 = new CitizenExtended()
			.personId(UUID.fromString(partyId2));

		final var result = CitizenCategorizationHelper.categorizeCitizens(
			List.of(citizen1, citizen2),
			mapping,
			citizen -> true);

		assertThat(result.eligibleAdults()).isEmpty();
		assertThat(result.ineligibleMinors()).hasSize(2);
		assertThat(result.notRegisteredInSweden()).isEmpty();
	}

	@Test
	void categorizeCitizens_withAllNotRegistered() {
		final var partyId1 = UUID.randomUUID().toString();
		final var partyId2 = UUID.randomUUID().toString();

		final var mapping = Map.of(
			partyId1, ADULT_LEGAL_ID,
			partyId2, ADULT_LEGAL_ID2);

		final var citizen1 = new CitizenExtended()
			.personId(UUID.fromString(partyId1));

		final var citizen2 = new CitizenExtended()
			.personId(UUID.fromString(partyId2));

		final var result = CitizenCategorizationHelper.categorizeCitizens(
			List.of(citizen1, citizen2),
			mapping,
			citizen -> false);

		assertThat(result.eligibleAdults()).isEmpty();
		assertThat(result.ineligibleMinors()).isEmpty();
		assertThat(result.notRegisteredInSweden()).hasSize(2);
	}
}
