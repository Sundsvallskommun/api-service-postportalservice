package se.sundsvall.postportalservice.integration.party;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

import static java.util.Collections.emptyMap;

@Component
public class PartyIntegration {

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
		return partyClient.getPartyIds(municipalityId, legalIds);
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
		return partyClient.getPersonNumbers(municipalityId, partyIds);
	}

}
