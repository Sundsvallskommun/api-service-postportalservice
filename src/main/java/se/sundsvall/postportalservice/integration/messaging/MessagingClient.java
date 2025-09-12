package se.sundsvall.postportalservice.integration.messaging;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.postportalservice.integration.messaging.configuration.MessagingConfiguration.CLIENT_ID;

import generated.se.sundsvall.messaging.DigitalMailRequest;
import generated.se.sundsvall.messaging.MessageBatchResult;
import generated.se.sundsvall.messaging.MessageResult;
import generated.se.sundsvall.messaging.SmsBatchRequest;
import generated.se.sundsvall.messaging.SmsRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.postportalservice.integration.messaging.configuration.MessagingConfiguration;

@FeignClient(
	name = CLIENT_ID,
	url = "${integration.messaging.url}",
	configuration = MessagingConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface MessagingClient {

	@PostMapping(path = "/{municipalityId}/digital-mail", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	MessageBatchResult sendDigitalMail(
		@RequestHeader(Identifier.HEADER_NAME) String identifier,
		@RequestHeader final String origin,
		@PathVariable final String municipalityId,
		@RequestBody final DigitalMailRequest request);

	@PostMapping(path = "/{municipalityId}/sms", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	MessageResult sendSms(
		@RequestHeader final String identifier,
		@RequestHeader final String origin,
		@PathVariable final String municipalityId,
		@RequestBody final SmsRequest request);

	@PostMapping(path = "/{municipalityId}/sms/batch", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	MessageBatchResult sendSmsBatch(
		@RequestHeader(Identifier.HEADER_NAME) String identifier,
		@RequestHeader final String origin,
		@PathVariable final String municipalityId,
		@RequestBody final SmsBatchRequest request);

	// TODO: Add the request body when implemented in messaging, also add the return type.
	@PostMapping(path = "/{municipalityId}/snail-mail", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	void sendSnailMail(
		@RequestHeader(Identifier.HEADER_NAME) String identifier,
		@RequestHeader final String origin,
		@PathVariable final String municipalityId);

	// TODO: Add the request body when implemented in messaging, also add the return type.
	@PostMapping(path = "/{municipalityId}/{organizationNumber}/mailboxes", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	void precheckMailboxes(
		@PathVariable final String municipalityId,
		@PathVariable final String organizationNumber);

}
