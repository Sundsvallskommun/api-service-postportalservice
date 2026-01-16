package se.sundsvall.postportalservice.integration.party;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.postportalservice.integration.party.configuration.PartyConfiguration.CLIENT_ID;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import se.sundsvall.postportalservice.integration.party.configuration.PartyConfiguration;

@FeignClient(
	name = CLIENT_ID,
	url = "${integration.party.url}",
	configuration = PartyConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface PartyClient {

	@PostMapping(path = "/{municipalityId}/PRIVATE/partyIds", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	Map<String, String> getPartyIds(
		@PathVariable final String municipalityId,
		@RequestBody final List<String> legalIds);
}
