package se.sundsvall.postportalservice.integration.party;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import se.sundsvall.postportalservice.integration.party.configuration.PartyProperties;

import static java.util.Collections.emptyMap;

@Component
public class PartyIntegration {

	private final PartyClient partyClient;
	private final PartyProperties partyProperties;

	public PartyIntegration(final PartyClient partyClient, PartyProperties partyProperties) {
		this.partyClient = partyClient;
		this.partyProperties = partyProperties;
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

		for (var i = 0; i < legalIds.size(); i += partyProperties.maxLegalIdsPerCall()) {
			final var partyIdsChunk = legalIds.subList(i, Math.min(i + partyProperties.maxLegalIdsPerCall(), legalIds.size()));
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

		for (var i = 0; i < partyIds.size(); i += partyProperties.maxPartyIdsPerCall()) {
			final var partyIdsChunk = partyIds.subList(i, Math.min(i + partyProperties.maxPartyIdsPerCall(), partyIds.size()));
			batchResult.putAll(partyClient.getPersonNumbers(municipalityId, partyIdsChunk));
		}

		return batchResult;
	}

}
