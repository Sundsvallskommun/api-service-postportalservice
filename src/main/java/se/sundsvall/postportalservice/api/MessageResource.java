package se.sundsvall.postportalservice.api;

import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static se.sundsvall.postportalservice.service.util.ParseUtil.parseLetterRequest;
import static se.sundsvall.postportalservice.service.util.ValidationUtil.validate;

import generated.se.sundsvall.messaging.ConstraintViolationProblem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.zalando.problem.Problem;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.postportalservice.api.model.Attachments;
import se.sundsvall.postportalservice.api.model.DigitalRegisteredLetterRequest;
import se.sundsvall.postportalservice.api.model.LetterRequest;
import se.sundsvall.postportalservice.api.model.SmsRequest;

@Validated
@RestController
@Tag(name = "Message Resources")
@RequestMapping("/{municipalityId}/messages")
@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
	Problem.class, ConstraintViolationProblem.class
})))
@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
class MessageResource {

	MessageResource() {}

	@Operation(summary = "Send a message by either digital mail or as a physical letter. Digital mail is always preferred if possible.", responses = {
		@ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
	})
	@PostMapping(value = "/letter", produces = ALL_VALUE)
	ResponseEntity<Void> sendLetter(
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@RequestPart(name = "request") @Schema(description = "Letter request as a JSON string", implementation = LetterRequest.class) final String request,
		@RequestPart(name = "attachments") final List<MultipartFile> files) {

		var letterRequest = parseLetterRequest(request);
		validate(letterRequest);

		var attachments = Attachments.create()
			.withFiles(files);
		validate(attachments);

		// Should return created and a location header.
		// /{municipalityId}/history/messages/{messageId}
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

	@Operation(summary = "Send a digital registered letter.", responses = {
		@ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
	})
	@PostMapping(value = "/registered-letter", produces = ALL_VALUE)
	ResponseEntity<Void> sendDigitalRegisteredLetter(
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@RequestPart(name = "request") @Schema(description = "Digital registered letter request as a JSON string", implementation = DigitalRegisteredLetterRequest.class) final String requestString,
		@RequestPart(name = "attachments") final List<MultipartFile> files) {

		// Should return created and a location header.
		// /{municipalityId}/history/messages/{messageId}
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

	@Operation(summary = "Send an SMS", responses = {
		@ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
	})
	@PostMapping(value = "/sms", produces = ALL_VALUE)
	ResponseEntity<Void> sendSms(
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@RequestBody @Valid final SmsRequest request) {

		// Should return created and a location header.
		// /{municipalityId}/history/messages/{messageId}
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

}
