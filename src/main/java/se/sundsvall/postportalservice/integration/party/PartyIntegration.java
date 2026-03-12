package se.sundsvall.postportalservice.integration.party;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

import static java.util.Collections.emptyMap;

@Component
public class PartyIntegration {

	private static final int MAX_PARTY_IDS_PER_CALL = 1000;
	private static final int MAX_LEGAL_IDS_PER_CALL = 1000;

	private final PartyClient partyClient;

	public PartyIntegration(final PartyClient partyClient) {
		this.partyClient = partyClient;
	}

	/**
	 * Get partyIds for the provided legalIds.
	 *
	 * @param  municipalityId the municipality id
	 * @param  legalIds       the legalIds
	 * @return                a map of legalId to partyId
	 */
	public Map<String, String> getPartyIds(final String municipalityId, final List<String> legalIds) {
		if (legalIds == null || legalIds.isEmpty()) {
			return emptyMap();
		}

		final var batchResult = new HashMap<String, String>();

		for (var i = 0; i < legalIds.size(); i += MAX_LEGAL_IDS_PER_CALL) {
			final var partyIdsChunk = legalIds.subList(i, Math.min(i + MAX_LEGAL_IDS_PER_CALL, legalIds.size()));
			batchResult.putAll(partyClient.getPartyIds(municipalityId, partyIdsChunk));
		}

		return batchResult;
	}

	/**
	 * Get legalIds for the provided partyIds.
	 *
	 * @param  municipalityId the municipality id
	 * @param  partyIds       the partyIds
	 * @return                a map of partyId to legalId
	 */
	public Map<String, String> getLegalIds(final String municipalityId, final List<String> partyIds) {
		if (partyIds == null || partyIds.isEmpty()) {
			return emptyMap();
		}

		final var batchResult = new HashMap<String, String>();

		for (var i = 0; i < partyIds.size(); i += MAX_PARTY_IDS_PER_CALL) {
			final var partyIdsChunk = partyIds.subList(i, Math.min(i + MAX_PARTY_IDS_PER_CALL, partyIds.size()));
			batchResult.putAll(partyClient.getPersonNumbers(municipalityId, partyIdsChunk));
		}

		return batchResult;
	}

}
