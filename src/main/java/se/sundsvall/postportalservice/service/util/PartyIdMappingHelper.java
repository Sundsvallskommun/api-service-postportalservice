package se.sundsvall.postportalservice.service.util;

import static java.util.Collections.emptyList;

import generated.se.sundsvall.citizen.PersonGuidBatch;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Utility class for extracting and mapping partyIds from PersonGuidBatch responses.
 */
public final class PartyIdMappingHelper {

	private PartyIdMappingHelper() {}

	/**
	 * Extracts partyIds from PersonGuidBatch responses.
	 *
	 * @param  batches the list of PersonGuidBatch objects
	 * @return         a list of partyIds as strings
	 */
	public static List<String> extractPartyIds(final List<PersonGuidBatch> batches) {
		return Optional.ofNullable(batches).orElse(emptyList()).stream()
			.filter(batch -> Boolean.TRUE.equals(batch.getSuccess()))
			.map(PersonGuidBatch::getPersonId)
			.filter(Objects::nonNull)
			.map(UUID::toString)
			.toList();
	}

	/**
	 * Creates a map from partyId to legalId for age verification.
	 *
	 * @param  batches the list of PersonGuidBatch objects
	 * @return         a map of partyId to legalId
	 */
	public static Map<String, String> createPartyIdTolegalIdMap(final List<PersonGuidBatch> batches) {
		return Optional.ofNullable(batches).orElse(emptyList()).stream()
			.filter(batch -> Boolean.TRUE.equals(batch.getSuccess()))
			.filter(batch -> batch.getPersonId() != null && batch.getPersonNumber() != null)
			.collect(Collectors.toMap(
				batch -> batch.getPersonId().toString(),
				PersonGuidBatch::getPersonNumber,
				(existing, replacement) -> existing));
	}

	/**
	 * Extracts both party IDs and creates legalId mapping in one operation.
	 *
	 * @param  batches the list of PersonGuidBatch objects
	 * @return         a PartyIdMapping record containing party IDs and mapping
	 */
	public static PartyIdMapping extractPartyIdMapping(final List<PersonGuidBatch> batches) {
		final var partyIds = extractPartyIds(batches);
		final var partyIdToLegalIdMap = createPartyIdTolegalIdMap(batches);
		return new PartyIdMapping(partyIds, partyIdToLegalIdMap);
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

		public void addToPartyIdToLegalIdMap(String partyId, String legalId) {
			this.partyIds.add(partyId);
			this.partyIdToLegalId.put(partyId, legalId);
		}
	}
}
