package se.sundsvall.postportalservice.integration.employee;

import generated.se.sundsvall.employee.PortalPersonData;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class EmployeeIntegration {

	private final EmployeeClient client;

	private static final String PORTAL_PERSON_DATA_CACHE = "portalPersonDataCache";

	public EmployeeIntegration(final EmployeeClient client) {
		this.client = client;
	}

	@Cacheable(value = PORTAL_PERSON_DATA_CACHE, key = "{#municipalityId, #userName}")
	public Optional<PortalPersonData> getPortalPersonData(final String municipalityId, final String userName) {
		return Optional.ofNullable(client.getPortalPersonData(municipalityId, "personal", userName));
	}

}
