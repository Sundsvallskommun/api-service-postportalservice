package se.sundsvall.postportalservice.integration.employee;

import static se.sundsvall.postportalservice.integration.employee.configuration.EmployeeConfiguration.CLIENT_ID;

import generated.se.sundsvall.employee.PortalPersonData;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import se.sundsvall.postportalservice.integration.employee.configuration.EmployeeConfiguration;

@FeignClient(
	name = CLIENT_ID,
	url = "${integration.employee.url}",
	configuration = EmployeeConfiguration.class,
	dismiss404 = true)
@CircuitBreaker(name = CLIENT_ID)
public interface EmployeeClient {

	@GetMapping(path = "/{municipalityId}/portalpersondata/{domain}/{loginName}")
	PortalPersonData getPortalPersonData(
		@PathVariable String municipalityId,
		@PathVariable String domain,
		@PathVariable String loginName);

}
