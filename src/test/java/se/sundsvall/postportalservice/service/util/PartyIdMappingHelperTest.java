package se.sundsvall.postportalservice.service.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PartyIdMappingHelperTest {

	private static final UUID PARTY_ID_1 = UUID.randomUUID();
	private static final UUID PARTY_ID_2 = UUID.randomUUID();
	private static final String LEGAL_ID_1 = "199001011234";
	private static final String LEGAL_ID_2 = "198501015678";

	@Test
	void extractPartyIdMappingFromMap_withValidMap() {
		final var legalIdToPartyIdMap = Map.of(
			LEGAL_ID_1, PARTY_ID_1.toString(),
			LEGAL_ID_2, PARTY_ID_2.toString());

		final var result = PartyIdMappingHelper.extractPartyIdMappingFromMap(legalIdToPartyIdMap);

		assertThat(result).isNotNull();
		assertThat(result.partyIds()).hasSize(2)
			.containsExactlyInAnyOrder(PARTY_ID_1.toString(), PARTY_ID_2.toString());
		assertThat(result.partyIdToLegalId()).hasSize(2)
			.containsEntry(PARTY_ID_1.toString(), LEGAL_ID_1)
			.containsEntry(PARTY_ID_2.toString(), LEGAL_ID_2);
	}

	@Test
	void extractPartyIdMappingFromMap_withNullValues() {
		final var legalIdToPartyIdMap = new HashMap<String, String>();
		legalIdToPartyIdMap.put(LEGAL_ID_1, PARTY_ID_1.toString());
		legalIdToPartyIdMap.put(LEGAL_ID_2, null);

		final var result = PartyIdMappingHelper.extractPartyIdMappingFromMap(legalIdToPartyIdMap);

		assertThat(result).isNotNull();
		assertThat(result.partyIds()).hasSize(1)
			.containsExactly(PARTY_ID_1.toString());
		assertThat(result.partyIdToLegalId()).hasSize(1)
			.containsEntry(PARTY_ID_1.toString(), LEGAL_ID_1);
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
		legalIdToPartyIdMap.put(LEGAL_ID_1, PARTY_ID_1.toString());
		legalIdToPartyIdMap.put(LEGAL_ID_2, PARTY_ID_1.toString());

		final var result = PartyIdMappingHelper.extractPartyIdMappingFromMap(legalIdToPartyIdMap);

		assertThat(result).isNotNull();
		assertThat(result.partyIds()).hasSize(2)
			.containsExactlyInAnyOrder(PARTY_ID_1.toString(), PARTY_ID_1.toString());
		assertThat(result.partyIdToLegalId()).hasSize(1)
			.containsKey(PARTY_ID_1.toString());
	}
}
