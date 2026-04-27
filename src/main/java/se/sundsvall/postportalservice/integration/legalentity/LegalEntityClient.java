package se.sundsvall.postportalservice.integration.legalentity;

import generated.se.sundsvall.legalentity.LegalEntity2;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import se.sundsvall.postportalservice.integration.legalentity.configuration.LegalEntityConfiguration;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.postportalservice.integration.legalentity.configuration.LegalEntityConfiguration.CLIENT_ID;

@CircuitBreaker(name = CLIENT_ID)
@FeignClient(
	name = CLIENT_ID,
	url = "${integration.legalentity.url}",
	configuration = LegalEntityConfiguration.class,
	dismiss404 = true)
public interface LegalEntityClient {

	@GetMapping(path = "/{municipalityId}/{legalEntityId}", produces = APPLICATION_JSON_VALUE)
	LegalEntity2 getLegalEntity(
		@PathVariable final String municipalityId,
		@PathVariable final String legalEntityId);
}
