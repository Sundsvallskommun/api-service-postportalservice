package se.sundsvall.postportalservice.api;

import generated.se.sundsvall.messaging.ConstraintViolationProblem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.postportalservice.api.model.SigningEvent;
import se.sundsvall.postportalservice.service.SigningEventService;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;

@Validated
@RestController
@Tag(name = "E-signing", description = "Inbound signing events from api-service-e-signing")
@RequestMapping("/{municipalityId}/e-signing")
@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
	Problem.class, ConstraintViolationProblem.class
})))
@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
class ESigningEventResource {

	private final SigningEventService signingEventService;

	ESigningEventResource(final SigningEventService signingEventService) {
		this.signingEventService = signingEventService;
	}

	@Operation(summary = "Receive a signing event and update the signing case identified by the message id", responses = {
		@ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
	})
	@PostMapping(value = "/events/{messageId}", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> receiveSigningEvent(
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "messageId", description = "The Postportalen message id the signing case belongs to", example = "550e8400-e29b-41d4-a716-446655440000") @ValidUuid @PathVariable final String messageId,
		@Valid @RequestBody final SigningEvent event) {
		signingEventService.handleSigningEvent(municipalityId, messageId, event);
		return ok().build();
	}
}
