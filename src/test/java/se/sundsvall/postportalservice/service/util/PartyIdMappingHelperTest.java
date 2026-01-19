package se.sundsvall.postportalservice.service.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PartyIdMappingHelperTest {

	private static final UUID PARTYID_1 = UUID.randomUUID();
	private static final UUID PARTYID_2 = UUID.randomUUID();
	private static final String LEGALID_1 = "199001011234";
	private static final String LEGALID_2 = "198501015678";

	@Test
	void extractPartyIdMappingFromMap_withValidMap() {
		final var legalIdToPartyIdMap = Map.of(
			LEGALID_1, PARTYID_1.toString(),
			LEGALID_2, PARTYID_2.toString());

		final var result = PartyIdMappingHelper.extractPartyIdMappingFromMap(legalIdToPartyIdMap);

		assertThat(result).isNotNull();
		assertThat(result.partyIds()).hasSize(2)
			.containsExactlyInAnyOrder(PARTYID_1.toString(), PARTYID_2.toString());
		assertThat(result.partyIdToLegalId()).hasSize(2)
			.containsEntry(PARTYID_1.toString(), LEGALID_1)
			.containsEntry(PARTYID_2.toString(), LEGALID_2);
	}

	@Test
	void extractPartyIdMappingFromMap_withNullValues() {
		final var legalIdToPartyIdMap = new HashMap<String, String>();
		legalIdToPartyIdMap.put(LEGALID_1, PARTYID_1.toString());
		legalIdToPartyIdMap.put(LEGALID_2, null);

		final var result = PartyIdMappingHelper.extractPartyIdMappingFromMap(legalIdToPartyIdMap);

		assertThat(result).isNotNull();
		assertThat(result.partyIds()).hasSize(1)
			.containsExactly(PARTYID_1.toString());
		assertThat(result.partyIdToLegalId()).hasSize(1)
			.containsEntry(PARTYID_1.toString(), LEGALID_1);
	}

	@Test
	void extractPartyIdMappingFromMap_withEmptyMap() {
		final var result = PartyIdMappingHelper.extractPartyIdMappingFromMap(Map.of());

		assertThat(result).isNotNull();
		assertThat(result.partyIds()).isEmpty();
		assertThat(result.partyIdToLegalId()).isEmpty();
	}

	@Test
	void extractPartyIdMappingFromMap_withDuplicatePartyIds() {
		// Two different legalIds mapping to the same partyId - first one wins
		final var legalIdToPartyIdMap = new HashMap<String, String>();
		legalIdToPartyIdMap.put(LEGALID_1, PARTYID_1.toString());
		legalIdToPartyIdMap.put(LEGALID_2, PARTYID_1.toString());

		final var result = PartyIdMappingHelper.extractPartyIdMappingFromMap(legalIdToPartyIdMap);

		assertThat(result).isNotNull();
		assertThat(result.partyIds()).hasSize(2)
			.containsExactlyInAnyOrder(PARTYID_1.toString(), PARTYID_1.toString());
		assertThat(result.partyIdToLegalId()).hasSize(1)
			.containsKey(PARTYID_1.toString());
	}
}
