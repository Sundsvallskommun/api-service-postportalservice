package se.sundsvall.postportalservice.integration.citizen;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

import generated.se.sundsvall.citizen.CitizenAddress;
import generated.se.sundsvall.citizen.CitizenExtended;
import generated.se.sundsvall.citizen.PersonGuidBatch;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CitizenIntegration {

	static final String POPULATION_REGISTRATION_ADDRESS = "POPULATION_REGISTRATION_ADDRESS";

	private final CitizenClient client;

	public CitizenIntegration(final CitizenClient client) {
		this.client = client;
	}

	public List<PersonGuidBatch> getPartyIds(final String municipalityId, final List<String> personIds) {
		if (personIds.isEmpty()) {
			return emptyList();
		}

		return client.getPartyIds(municipalityId, personIds);
	}

	public List<CitizenExtended> getCitizens(final String municipalityId, final List<String> partyIds) {
		if (partyIds.isEmpty()) {
			return emptyList();
		}

		return client.getCitizens(municipalityId, partyIds);
	}

	public Optional<CitizenAddress> getPopulationRegistrationAddress(final CitizenExtended citizen) {
		if (citizen == null) {
			return Optional.empty();
		}

		return ofNullable(citizen.getAddresses())
			.orElse(emptyList())
			.stream()
			.filter(address -> POPULATION_REGISTRATION_ADDRESS.equals(address.getAddressType()))
			.findFirst();
	}

	public boolean isRegisteredInSweden(final CitizenExtended citizen) {
		return getPopulationRegistrationAddress(citizen).isPresent();
	}
}
