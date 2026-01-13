package se.sundsvall.postportalservice.integration.citizen;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

import generated.se.sundsvall.citizen.CitizenExtended;
import generated.se.sundsvall.citizen.PersonGuidBatch;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CitizenIntegration {

	public static final String POPULATION_REGISTRATION_ADDRESS = "POPULATION_REGISTRATION_ADDRESS";

	private final CitizenClient client;

	public CitizenIntegration(final CitizenClient client) {
		this.client = client;
	}

	public List<PersonGuidBatch> getPartyIds(final String municipalityId, final List<String> personIds) {
		if (personIds == null || personIds.isEmpty()) {
			return emptyList();
		}

		return client.getPartyIds(municipalityId, personIds);
	}

	public List<CitizenExtended> getCitizens(final String municipalityId, final List<String> partyIds) {
		if (ofNullable(partyIds).orElse(emptyList()).isEmpty()) {
			return emptyList();
		}

		return client.getCitizens(municipalityId, partyIds);
	}

	public List<PersonGuidBatch> getPersonNumbers(final String municipalityId, final List<String> partyIds) {
		if (ofNullable(partyIds).orElse(emptyList()).isEmpty()) {
			return emptyList();
		}

		return client.getPersonNumbers(municipalityId, partyIds);
	}
}
