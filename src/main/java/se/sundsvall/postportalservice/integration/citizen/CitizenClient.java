package se.sundsvall.postportalservice.integration.citizen;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.postportalservice.integration.citizen.configuration.CitizenConfiguration.CLIENT_ID;

import generated.se.sundsvall.citizen.CitizenExtended;
import generated.se.sundsvall.citizen.PersonGuidBatch;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import se.sundsvall.postportalservice.integration.citizen.configuration.CitizenConfiguration;

@CircuitBreaker(name = CLIENT_ID)
@FeignClient(
	name = CLIENT_ID,
	url = "${integration.citizen.url}",
	configuration = CitizenConfiguration.class)
public interface CitizenClient {

	@PostMapping(
		path = "/{municipalityId}/batch",
		consumes = APPLICATION_JSON_VALUE,
		produces = APPLICATION_JSON_VALUE)
	List<CitizenExtended> getCitizens(
		@PathVariable("municipalityId") String municipalityId,
		@RequestBody List<String> personIds);

	@PostMapping(
		path = "/{municipalityId}/guid/batch",
		consumes = APPLICATION_JSON_VALUE,
		produces = APPLICATION_JSON_VALUE)
	List<PersonGuidBatch> getPartyIds(
		@PathVariable("municipalityId") String municipalityId,
		@RequestBody List<String> personIds);
}
