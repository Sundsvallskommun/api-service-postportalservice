package se.sundsvall.postportalservice.integration.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import generated.se.sundsvall.employee.PortalPersonData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeeIntegrationTest {

	@Mock
	private EmployeeClient employeeClient;

	@InjectMocks
	private EmployeeIntegration employeeIntegration;

	@Test
	void getPortalPersonData() {
		var municipalityId = "2281";
		var userName = "username";
		var portalPersonData = new PortalPersonData();

		when(employeeClient.getPortalPersonData(municipalityId, "personal", userName)).thenReturn(portalPersonData);

		var result = employeeIntegration.getPortalPersonData(municipalityId, userName);

		assertThat(result).isPresent();
		assertThat(result.get()).isEqualTo(portalPersonData);
		verify(employeeClient).getPortalPersonData(municipalityId, "personal", userName);
		verifyNoMoreInteractions(employeeClient);
	}

}
