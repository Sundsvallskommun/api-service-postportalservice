package se.sundsvall.postportalservice.integration.party;

import static java.util.Collections.emptyMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class PartyIntegration {

	private final PartyClient partyClient;

	public PartyIntegration(final PartyClient partyClient) {
		this.partyClient = partyClient;
	}

	public Map<String, String> getPartyIds(final String municipalityId, final Set<String> legalIds) {
		return partyClient.getPartyIds(municipalityId, new ArrayList<>(legalIds));
	}

	public Map<String, String> getPartyIds(final String municipalityId, final List<String> legalIds) {
		if (legalIds == null || legalIds.isEmpty()) {
			return emptyMap();
		}
		return partyClient.getPartyIds(municipalityId, legalIds);
	}

	public Map<String, String> getPersonNumbers(final String municipalityId, final List<String> partyIds) {
		if (partyIds == null || partyIds.isEmpty()) {
			return emptyMap();
		}
		return partyClient.getPersonNumbers(municipalityId, partyIds);
	}

}
