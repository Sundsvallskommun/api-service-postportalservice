package se.sundsvall.postportalservice.api;

import generated.se.sundsvall.messaging.ConstraintViolationProblem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.postportalservice.api.model.RecipientResponse;
import se.sundsvall.postportalservice.service.RecipientService;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;

@Validated
@RestController
@Tag(name = "Recipient Resources")
@RequestMapping("/{municipalityId}/recipients")
@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
	Problem.class, ConstraintViolationProblem.class
})))
@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
class RecipientResource {

	private final RecipientService recipientService;

	RecipientResource(final RecipientService recipientService) {
		this.recipientService = recipientService;
	}

	@Operation(summary = "Get recipient data for a digital registered letter", description = "Returns all metadata needed to send a digital registered letter for the given recipient")
	@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true)
	@GetMapping(value = "/{recipientId}", produces = APPLICATION_JSON_VALUE)
	ResponseEntity<RecipientResponse> getRecipientById(
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "recipientId", description = "Recipient ID", example = "da03b33e-9de2-45ac-8291-31a88de59410") @PathVariable final String recipientId) {

		return ResponseEntity.ok(recipientService.getRecipient(municipalityId, recipientId));
	}

}
