package se.sundsvall.postportalservice.integration.party;

import java.util.ArrayList;
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

}
