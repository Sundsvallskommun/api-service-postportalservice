package se.sundsvall.postportalservice.api;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;

import generated.se.sundsvall.messaging.ConstraintViolationProblem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.zalando.problem.Problem;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.postportalservice.api.model.DigitalRegisteredLetterRequest;
import se.sundsvall.postportalservice.api.model.LetterCsvRequest;
import se.sundsvall.postportalservice.api.model.LetterRequest;
import se.sundsvall.postportalservice.api.model.SmsRequest;
import se.sundsvall.postportalservice.api.validation.NoDuplicateFileNames;
import se.sundsvall.postportalservice.api.validation.ValidCsv;
import se.sundsvall.postportalservice.api.validation.ValidIdentifier;
import se.sundsvall.postportalservice.api.validation.ValidPdf;
import se.sundsvall.postportalservice.service.MessageService;

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

	private final MessageService messageService;

	MessageResource(final MessageService messageService) {
		this.messageService = messageService;
	}

	@Operation(summary = "Send a message by either digital mail or as a physical letter. Digital mail is always preferred if possible.", responses = {
		@ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
	})
	@PostMapping(value = "/letter", produces = ALL_VALUE)
	ResponseEntity<Void> sendLetter(
		@RequestHeader(value = Identifier.HEADER_NAME) @ValidIdentifier final String xSentBy,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@RequestPart(name = "request") @Valid final LetterRequest request,
		@RequestPart(name = "attachments") @NotEmpty @NoDuplicateFileNames final List<MultipartFile> attachments) {
		Identifier.set(Identifier.parse(xSentBy));

		final var messageId = messageService.processLetterRequest(municipalityId, request, attachments);

		return created(fromPath("/{municipalityId}/history/messages/{messageId}")
			.buildAndExpand(municipalityId, messageId).toUri())
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}

	@Operation(summary = "Send a message by either digital mail or as a physical letter. Digital mail is always preferred if possible.", responses = {
		@ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
	})
	@PostMapping("/letter/csv")
	ResponseEntity<Void> sendLetterCSV(
		@RequestHeader(value = Identifier.HEADER_NAME) @ValidIdentifier final String xSentBy,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@RequestPart(name = "request") @Valid final LetterCsvRequest request,
		@RequestPart(name = "csv-file") @ValidCsv final MultipartFile csvFile,
		@RequestPart(name = "attachments", required = false) @NotEmpty @NoDuplicateFileNames final List<MultipartFile> attachments) {
		Identifier.set(Identifier.parse(xSentBy));

		final var messageId = messageService.processCsvLetterRequest(municipalityId, request, csvFile, attachments);

		return created(fromPath("/{municipalityId}/history/messages/{messageId}")
			.buildAndExpand(municipalityId, messageId).toUri())
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}

	@Operation(summary = "Send a digital registered letter.", responses = {
		@ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
	})
	@PostMapping(value = "/registered-letter", produces = ALL_VALUE, consumes = MULTIPART_FORM_DATA_VALUE)
	ResponseEntity<Void> sendDigitalRegisteredLetter(
		@RequestHeader(value = Identifier.HEADER_NAME) @ValidIdentifier final String xSentBy,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@RequestPart(name = "request") @Valid final DigitalRegisteredLetterRequest request,
		@RequestPart(name = "attachments") @ValidPdf @NotEmpty @NoDuplicateFileNames final List<MultipartFile> attachments) {
		Identifier.set(Identifier.parse(xSentBy));

		final var messageId = messageService.processDigitalRegisteredLetterRequest(municipalityId, request, attachments);

		return created(fromPath("/{municipalityId}/history/messages/{messageId}")
			.buildAndExpand(municipalityId, messageId).toUri())
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}

	@Operation(summary = "Send an SMS", responses = {
		@ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
	})
	@PostMapping(value = "/sms", produces = ALL_VALUE)
	ResponseEntity<Void> sendSms(
		@RequestHeader(value = Identifier.HEADER_NAME) @ValidIdentifier final String xSentBy,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@RequestBody @Valid final SmsRequest request) {
		Identifier.set(Identifier.parse(xSentBy));

		final var messageId = messageService.processSmsRequest(municipalityId, request);

		return created(fromPath("/{municipalityId}/history/messages/{messageId}")
			.buildAndExpand(municipalityId, messageId).toUri())
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}

}
