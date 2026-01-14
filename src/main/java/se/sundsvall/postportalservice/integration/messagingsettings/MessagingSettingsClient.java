package se.sundsvall.postportalservice.integration.messagingsettings;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.postportalservice.integration.messagingsettings.configuration.MessagingSettingsConfiguration.CLIENT_ID;

import generated.se.sundsvall.messagingsettings.MessagingSettings;
import generated.se.sundsvall.messagingsettings.SenderInfoResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.postportalservice.integration.messagingsettings.configuration.MessagingSettingsConfiguration;

@CircuitBreaker(name = CLIENT_ID)
@FeignClient(
	name = CLIENT_ID,
	url = "${integration.messagingsettings.url}",
	configuration = MessagingSettingsConfiguration.class,
	dismiss404 = true)
public interface MessagingSettingsClient {

	@GetMapping(path = "/{municipalityId}/sender-info", produces = APPLICATION_JSON_VALUE)
	List<SenderInfoResponse> getSenderInfo(
		@PathVariable("municipalityId") String municipalityId,
		@RequestParam("departmentId") String departmentId);

	@GetMapping(path = "/{municipalityId}/user", produces = APPLICATION_JSON_VALUE)
	List<MessagingSettings> getMessagingSettingsForUser(
		@RequestHeader(Identifier.HEADER_NAME) final String xSentBy,
		@PathVariable("municipalityId") final String municipalityId);
}
