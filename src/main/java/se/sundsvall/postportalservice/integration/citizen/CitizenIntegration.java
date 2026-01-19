package se.sundsvall.postportalservice.integration.citizen;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

import generated.se.sundsvall.citizen.CitizenExtended;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CitizenIntegration {

	public static final String POPULATION_REGISTRATION_ADDRESS = "POPULATION_REGISTRATION_ADDRESS";

	private final CitizenClient client;

	public CitizenIntegration(final CitizenClient client) {
		this.client = client;
	}

	public List<CitizenExtended> getCitizens(final String municipalityId, final List<String> partyIds) {
		if (ofNullable(partyIds).orElse(emptyList()).isEmpty()) {
			return emptyList();
		}

		return client.getCitizens(municipalityId, partyIds);
	}
}
