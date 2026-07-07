package se.sundsvall.postportalservice.integration.esigning;

import generated.se.sundsvall.esigning.StartSigningRequest;
import generated.se.sundsvall.esigning.StartSigningResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import se.sundsvall.postportalservice.integration.esigning.configuration.EsigningConfiguration;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.postportalservice.integration.esigning.configuration.EsigningConfiguration.CLIENT_ID;

@FeignClient(
	name = CLIENT_ID,
	url = "${integration.esigning.url}",
	configuration = EsigningConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface EsigningClient {

	@PostMapping(path = "/{municipalityId}/e-signing/signings", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	StartSigningResponse createSigning(
		@PathVariable final String municipalityId,
		@RequestBody final StartSigningRequest request);

}
