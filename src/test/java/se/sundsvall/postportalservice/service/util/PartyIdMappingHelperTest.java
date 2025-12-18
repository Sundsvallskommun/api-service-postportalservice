package se.sundsvall.postportalservice.service.util;

import static org.assertj.core.api.Assertions.assertThat;

import generated.se.sundsvall.citizen.PersonGuidBatch;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PartyIdMappingHelperTest {

	private static final UUID PARTYID_1 = UUID.randomUUID();
	private static final UUID PARTYID_2 = UUID.randomUUID();
	private static final String LEGALID_1 = "199001011234";
	private static final String LEGALID_2 = "198501015678";

	@Test
	void extractPartyIds_withSuccessfulBatches() {
		final var batch1 = new PersonGuidBatch()
			.success(Boolean.TRUE)
			.personNumber(LEGALID_1)
			.personId(PARTYID_1);

		final var batch2 = new PersonGuidBatch()
			.success(Boolean.TRUE)
			.personNumber(LEGALID_2)
			.personId(PARTYID_2);

		final var result = PartyIdMappingHelper.extractPartyIds(List.of(batch1, batch2));

		assertThat(result).hasSize(2)
			.containsExactly(
				PARTYID_1.toString(),
				PARTYID_2.toString());
	}

	@Test
	void extractPartyIds_withFailedBatches() {
		final var successBatch = new PersonGuidBatch()
			.success(Boolean.TRUE)
			.personNumber(LEGALID_1)
			.personId(PARTYID_1);

		final var failedBatch = new PersonGuidBatch()
			.success(Boolean.FALSE)
			.personNumber(LEGALID_2)
			.personId(PARTYID_2);

		final var result = PartyIdMappingHelper.extractPartyIds(List.of(successBatch, failedBatch));

		assertThat(result).hasSize(1)
			.containsExactly(PARTYID_1.toString());
	}

	@Test
	void extractPartyIds_withNullPersonId() {
		final var batch = new PersonGuidBatch()
			.success(Boolean.TRUE)
			.personNumber(LEGALID_1)
			.personId(null);

		final var result = PartyIdMappingHelper.extractPartyIds(List.of(batch));

		assertThat(result).isEmpty();
	}

	@Test
	void extractPartyIds_withNullBatches() {
		final var result = PartyIdMappingHelper.extractPartyIds(null);

		assertThat(result).isEmpty();
	}

	@Test
	void extractPartyIds_withEmptyList() {
		final var result = PartyIdMappingHelper.extractPartyIds(List.of());

		assertThat(result).isEmpty();
	}

	@Test
	void createPartyIdToPersonnummerMap_withSuccessfulBatches() {
		final var batch1 = new PersonGuidBatch()
			.success(Boolean.TRUE)
			.personNumber(LEGALID_1)
			.personId(PARTYID_1);

		final var batch2 = new PersonGuidBatch()
			.success(Boolean.TRUE)
			.personNumber(LEGALID_2)
			.personId(PARTYID_2);

		final var result = PartyIdMappingHelper.createPartyIdTolegalIdMap(List.of(batch1, batch2));

		assertThat(result).hasSize(2)
			.containsEntry(PARTYID_1.toString(), LEGALID_1)
			.containsEntry(PARTYID_2.toString(), LEGALID_2);
	}

	@Test
	void createPartyIdToPersonnummerMap_withFailedBatches() {
		final var successBatch = new PersonGuidBatch()
			.success(Boolean.TRUE)
			.personNumber(LEGALID_1)
			.personId(PARTYID_1);

		final var failedBatch = new PersonGuidBatch()
			.success(Boolean.FALSE)
			.personNumber(LEGALID_2)
			.personId(PARTYID_2);

		final var result = PartyIdMappingHelper.createPartyIdTolegalIdMap(List.of(successBatch, failedBatch));

		assertThat(result).hasSize(1)
			.containsEntry(PARTYID_1.toString(), LEGALID_1);
	}

	@Test
	void createPartyIdToPersonnummerMap_withNullPersonId() {
		final var batch = new PersonGuidBatch()
			.success(Boolean.TRUE)
			.personNumber(LEGALID_1)
			.personId(null);

		final var result = PartyIdMappingHelper.createPartyIdTolegalIdMap(List.of(batch));

		assertThat(result).isEmpty();
	}

	@Test
	void createPartyIdToPersonnummerMap_withNullPersonNumber() {
		final var batch = new PersonGuidBatch()
			.success(Boolean.TRUE)
			.personNumber(null)
			.personId(PARTYID_1);

		final var result = PartyIdMappingHelper.createPartyIdTolegalIdMap(List.of(batch));

		assertThat(result).isEmpty();
	}

	@Test
	void createPartyIdToPersonnummerMap_withDuplicatePartyIds() {
		final var batch1 = new PersonGuidBatch()
			.success(Boolean.TRUE)
			.personNumber(LEGALID_1)
			.personId(PARTYID_1);

		final var batch2 = new PersonGuidBatch()
			.success(Boolean.TRUE)
			.personNumber(LEGALID_2)
			.personId(PARTYID_1);

		final var result = PartyIdMappingHelper.createPartyIdTolegalIdMap(List.of(batch1, batch2));

		assertThat(result).hasSize(1)
			.containsEntry(PARTYID_1.toString(), LEGALID_1);
	}

	@Test
	void createPartyIdToPersonnummerMap_withNullBatches() {
		final var result = PartyIdMappingHelper.createPartyIdTolegalIdMap(null);

		assertThat(result).isEmpty();
	}

	@Test
	void createPartyIdToPersonnummerMap_withEmptyList() {
		final var result = PartyIdMappingHelper.createPartyIdTolegalIdMap(List.of());

		assertThat(result).isEmpty();
	}

	@Test
	void extractPartyIdMapping_withSuccessfulBatches() {
		final var batch1 = new PersonGuidBatch()
			.success(Boolean.TRUE)
			.personNumber(LEGALID_1)
			.personId(PARTYID_1);

		final var batch2 = new PersonGuidBatch()
			.success(Boolean.TRUE)
			.personNumber(LEGALID_2)
			.personId(PARTYID_2);

		final var result = PartyIdMappingHelper.extractPartyIdMapping(List.of(batch1, batch2));

		assertThat(result).isNotNull();
		assertThat(result.partyIds()).hasSize(2)
			.containsExactly(
				PARTYID_1.toString(),
				PARTYID_2.toString());
		assertThat(result.partyIdToLegalId()).hasSize(2)
			.containsEntry(PARTYID_1.toString(), LEGALID_1)
			.containsEntry(PARTYID_2.toString(), LEGALID_2);
	}

	@Test
	void extractPartyIdMapping_withNullBatches() {
		final var result = PartyIdMappingHelper.extractPartyIdMapping(null);

		assertThat(result).isNotNull();
		assertThat(result.partyIds()).isEmpty();
		assertThat(result.partyIdToLegalId()).isEmpty();
	}

	@Test
	void extractPartyIdMapping_withEmptyList() {
		final var result = PartyIdMappingHelper.extractPartyIdMapping(List.of());

		assertThat(result).isNotNull();
		assertThat(result.partyIds()).isEmpty();
		assertThat(result.partyIdToLegalId()).isEmpty();
	}
}
