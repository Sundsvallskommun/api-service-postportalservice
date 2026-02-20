package se.sundsvall.postportalservice.api;

import generated.se.sundsvall.messaging.ConstraintViolationProblem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.postportalservice.api.model.MessageDetails;
import se.sundsvall.postportalservice.api.model.Messages;
import se.sundsvall.postportalservice.api.model.SigningInformation;
import se.sundsvall.postportalservice.service.HistoryService;

import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;

@Validated
@RestController
@Tag(name = "History Resources")
@RequestMapping("/{municipalityId}/history")
@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
	Problem.class, ConstraintViolationProblem.class
})))
@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
class HistoryResource {

	private final HistoryService historyService;

	HistoryResource(final HistoryService historyService) {
		this.historyService = historyService;
	}

	@Operation(summary = "Get messages sent by user", responses = {
		@ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
	})
	@GetMapping(value = "/users/{userId}/messages", produces = APPLICATION_JSON_VALUE)
	ResponseEntity<Messages> getUserMessages(
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "userId", description = "Username for user to fetch messages for", example = "joe01doe") @PathVariable("userId") final String username, final Pageable pageable) {

		return ok(historyService.getUserMessages(municipalityId, username, pageable));
	}

	@Operation(summary = "Get messages details", responses = {
		@ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
	})
	@GetMapping(value = "/users/{userId}/messages/{messageId}", produces = APPLICATION_JSON_VALUE)
	ResponseEntity<MessageDetails> getMessageDetails(
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "userId", description = "Username for the user who sent the message", example = "joe01doe") @PathVariable("userId") final String username,
		@Parameter(name = "messageId", description = "Message ID", example = "9ce333ec-a473-438b-8406-a71e957dc107") @PathVariable @ValidUuid final String messageId) {

		return ok(historyService.getMessageDetails(municipalityId, username, messageId));
	}

	@GetMapping(value = "/messages/{messageId}/signinginfo", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Get signing information", description = "Retrieves signing information connected to letter matching provided id", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation - OK", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<SigningInformation> getSigningInformation(
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "messageId", description = "Message ID", example = "9ce333ec-a473-438b-8406-a71e957dc107") @PathVariable @ValidUuid final String messageId) {
		return ok(historyService.getSigningInformation(municipalityId, messageId));
	}

	@GetMapping(value = "/messages/{messageId}/receipt", produces = ALL_VALUE)
	@Operation(summary = "Read digital registered letter receipt with the complete letter", description = "Retrieves letter receipt combined with the letter", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation - OK", content = @Content(mediaType = ALL_VALUE, schema = @Schema(type = "string", format = "binary"))),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<StreamingResponseBody> readLetterReceipt(
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "messageId", description = "Message ID", example = "9ce333ec-a473-438b-8406-a71e957dc107") @PathVariable @ValidUuid final String messageId) {

		return historyService.getLetterReceipt(municipalityId, messageId);
	}

}
