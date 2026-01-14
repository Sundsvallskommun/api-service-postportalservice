package se.sundsvall.postportalservice.service.util;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.postportalservice.TestDataFactory.generateLegalId;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import se.sundsvall.postportalservice.service.util.CitizenCategorizationHelper.SimplifiedCitizen;

class CitizenCategorizationHelperTest {

	private static final String ADULT_LEGAL_ID = generateLegalId(30, "0000"); // Adult
	private static final String ADULT_LEGAL_ID2 = generateLegalId(29, "0000"); // Adult #2
	private static final String MINOR_LEGAL_ID = generateLegalId(15, "0000"); // Minor
	private static final String MINOR_LEGAL_ID2 = generateLegalId(13, "0000"); // Minor #2

	@Test
	void isAdult_withAdultCitizen() {
		final var partyId = UUID.randomUUID().toString();
		final var citizen = new SimplifiedCitizen(partyId, ADULT_LEGAL_ID, true);

		final var predicate = CitizenCategorizationHelper.isAdult();

		assertThat(predicate.test(citizen)).isTrue();
	}

	@Test
	void isAdult_withMinorCitizen() {
		final var partyId = UUID.randomUUID().toString();
		final var citizen = new SimplifiedCitizen(partyId, MINOR_LEGAL_ID, true);

		final var predicate = CitizenCategorizationHelper.isAdult();

		assertThat(predicate.test(citizen)).isFalse();
	}

	@Test
	void isAdult_withNullLegalId() {
		final var partyId = UUID.randomUUID().toString();
		final var citizen = new SimplifiedCitizen(partyId, null, true);

		final var predicate = CitizenCategorizationHelper.isAdult();

		assertThat(predicate.test(citizen)).isFalse();
	}

	@Test
	void isAdult_withNullCitizen() {
		final var predicate = CitizenCategorizationHelper.isAdult();

		assertThat(predicate.test(null)).isFalse();
	}

	@Test
	void isMinor_withMinorCitizen() {
		final var partyId = UUID.randomUUID().toString();
		final var citizen = new SimplifiedCitizen(partyId, MINOR_LEGAL_ID, true);

		final var predicate = CitizenCategorizationHelper.isMinor();

		assertThat(predicate.test(citizen)).isTrue();
	}

	@Test
	void isMinor_withAdultCitizen() {
		final var partyId = UUID.randomUUID().toString();
		final var citizen = new SimplifiedCitizen(partyId, ADULT_LEGAL_ID, true);

		final var predicate = CitizenCategorizationHelper.isMinor();

		assertThat(predicate.test(citizen)).isFalse();
	}

	@Test
	void isMinor_withNullLegalId() {
		final var partyId = UUID.randomUUID().toString();
		final var citizen = new SimplifiedCitizen(partyId, null, true);

		final var predicate = CitizenCategorizationHelper.isMinor();

		assertThat(predicate.test(citizen)).isFalse();
	}

	@Test
	void isMinor_withNullCitizen() {
		final var predicate = CitizenCategorizationHelper.isMinor();

		assertThat(predicate.test(null)).isFalse();
	}

	@Test
	void categorizeCitizens_withMixedCitizens() {
		final var adultPartyId = UUID.randomUUID().toString();
		final var minorPartyId = UUID.randomUUID().toString();
		final var notInSwedenPartyId = UUID.randomUUID().toString();

		final var adultCitizen = new SimplifiedCitizen(adultPartyId, ADULT_LEGAL_ID, true);
		final var minorCitizen = new SimplifiedCitizen(minorPartyId, MINOR_LEGAL_ID, true);
		final var notInSwedenCitizen = new SimplifiedCitizen(notInSwedenPartyId, ADULT_LEGAL_ID2, false);

		final var result = CitizenCategorizationHelper.categorizeCitizens(
			List.of(adultCitizen, minorCitizen, notInSwedenCitizen));

		assertThat(result.eligibleAdults()).hasSize(1)
			.extracting(SimplifiedCitizen::partyId)
			.containsExactly(adultPartyId);

		assertThat(result.ineligibleMinors()).hasSize(1)
			.extracting(SimplifiedCitizen::partyId)
			.containsExactly(minorPartyId);

		assertThat(result.notRegisteredInSweden()).hasSize(1)
			.extracting(SimplifiedCitizen::partyId)
			.containsExactly(notInSwedenPartyId);
	}

	@Test
	void categorizeCitizens_withNullCitizens() {
		final var result = CitizenCategorizationHelper.categorizeCitizens(null);

		assertThat(result.eligibleAdults()).isEmpty();
		assertThat(result.ineligibleMinors()).isEmpty();
		assertThat(result.notRegisteredInSweden()).isEmpty();
	}

	@Test
	void categorizeCitizens_withEmptyList() {
		final var result = CitizenCategorizationHelper.categorizeCitizens(List.of());

		assertThat(result.eligibleAdults()).isEmpty();
		assertThat(result.ineligibleMinors()).isEmpty();
		assertThat(result.notRegisteredInSweden()).isEmpty();
	}

	@Test
	void categorizeCitizens_withAllAdults() {
		final var partyId1 = UUID.randomUUID().toString();
		final var partyId2 = UUID.randomUUID().toString();

		final var citizen1 = new SimplifiedCitizen(partyId1, ADULT_LEGAL_ID, true);
		final var citizen2 = new SimplifiedCitizen(partyId2, ADULT_LEGAL_ID2, true);

		final var result = CitizenCategorizationHelper.categorizeCitizens(
			List.of(citizen1, citizen2));

		assertThat(result.eligibleAdults()).hasSize(2);
		assertThat(result.ineligibleMinors()).isEmpty();
		assertThat(result.notRegisteredInSweden()).isEmpty();
	}

	@Test
	void categorizeCitizens_withAllMinors() {
		final var partyId1 = UUID.randomUUID().toString();
		final var partyId2 = UUID.randomUUID().toString();

		final var citizen1 = new SimplifiedCitizen(partyId1, MINOR_LEGAL_ID, true);
		final var citizen2 = new SimplifiedCitizen(partyId2, MINOR_LEGAL_ID2, true);

		final var result = CitizenCategorizationHelper.categorizeCitizens(
			List.of(citizen1, citizen2));

		assertThat(result.eligibleAdults()).isEmpty();
		assertThat(result.ineligibleMinors()).hasSize(2);
		assertThat(result.notRegisteredInSweden()).isEmpty();
	}

	@Test
	void categorizeCitizens_withAllNotRegistered() {
		final var partyId1 = UUID.randomUUID().toString();
		final var partyId2 = UUID.randomUUID().toString();

		final var citizen1 = new SimplifiedCitizen(partyId1, ADULT_LEGAL_ID, false);
		final var citizen2 = new SimplifiedCitizen(partyId2, ADULT_LEGAL_ID2, false);

		final var result = CitizenCategorizationHelper.categorizeCitizens(
			List.of(citizen1, citizen2));

		assertThat(result.eligibleAdults()).isEmpty();
		assertThat(result.ineligibleMinors()).isEmpty();
		assertThat(result.notRegisteredInSweden()).hasSize(2);
	}

	@Test
	void categorizeCitizens_withMixedRegistrationStatus() {
		final var adultInSwedenPartyId = UUID.randomUUID().toString();
		final var adultNotInSwedenPartyId = UUID.randomUUID().toString();
		final var minorInSwedenPartyId = UUID.randomUUID().toString();
		final var minorNotInSwedenPartyId = UUID.randomUUID().toString();

		final var adultInSweden = new SimplifiedCitizen(adultInSwedenPartyId, ADULT_LEGAL_ID, true);
		final var adultNotInSweden = new SimplifiedCitizen(adultNotInSwedenPartyId, ADULT_LEGAL_ID2, false);
		final var minorInSweden = new SimplifiedCitizen(minorInSwedenPartyId, MINOR_LEGAL_ID, true);
		final var minorNotInSweden = new SimplifiedCitizen(minorNotInSwedenPartyId, MINOR_LEGAL_ID2, false);

		final var result = CitizenCategorizationHelper.categorizeCitizens(
			List.of(adultInSweden, adultNotInSweden, minorInSweden, minorNotInSweden));

		assertThat(result.eligibleAdults()).hasSize(1)
			.extracting(SimplifiedCitizen::partyId)
			.containsExactly(adultInSwedenPartyId);

		assertThat(result.ineligibleMinors()).hasSize(1)
			.extracting(SimplifiedCitizen::partyId)
			.containsExactly(minorInSwedenPartyId);

		assertThat(result.notRegisteredInSweden()).hasSize(2)
			.extracting(SimplifiedCitizen::partyId)
			.containsExactlyInAnyOrder(adultNotInSwedenPartyId, minorNotInSwedenPartyId);
	}
}
