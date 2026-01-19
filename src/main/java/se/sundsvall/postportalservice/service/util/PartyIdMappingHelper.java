package se.sundsvall.postportalservice.service.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Utility class for extracting and mapping partyIds from PersonGuidBatch responses.
 */
public final class PartyIdMappingHelper {

	private PartyIdMappingHelper() {}

	/**
	 * Creates PartyIdMapping from a legalId to partyId map.
	 *
	 * @param  legalIdToPartyIdMap map from legalId to partyId
	 * @return                     a PartyIdMapping record containing party IDs and reversed mapping
	 */
	public static PartyIdMapping extractPartyIdMappingFromMap(final Map<String, String> legalIdToPartyIdMap) {
		final var partyIds = legalIdToPartyIdMap.values().stream()
			.filter(Objects::nonNull)
			.toList();

		// Reverse the map: legalId -> partyId becomes partyId -> legalId
		final var partyIdToLegalIdMap = legalIdToPartyIdMap.entrySet().stream()
			.filter(entry -> entry.getValue() != null)
			.collect(Collectors.toMap(
				Map.Entry::getValue,
				Map.Entry::getKey,
				(existing, replacement) -> existing));

		return new PartyIdMapping(new ArrayList<>(partyIds), partyIdToLegalIdMap);
	}

	/**
	 * Record holding both party IDs and partyId-to-legalId mapping.
	 *
	 * @param partyIds         list of party IDs
	 * @param partyIdToLegalId map from party ID to legalId
	 */
	public record PartyIdMapping(List<String> partyIds, Map<String, String> partyIdToLegalId) {

		public PartyIdMapping() {
			this(new ArrayList<>(), new HashMap<>());
		}

		public void addToPartyIdToLegalIdMap(final String partyId, final String legalId) {
			this.partyIds.add(partyId);
			this.partyIdToLegalId.put(partyId, legalId);
		}
	}
}
