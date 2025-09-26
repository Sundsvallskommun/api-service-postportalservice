package se.sundsvall.postportalservice.integration.digitalregisteredletter;

import static org.apache.commons.fileupload.FileUploadBase.MULTIPART_FORM_DATA;
import static se.sundsvall.postportalservice.integration.digitalregisteredletter.configuration.DigitalRegisteredLetterConfiguration.CLIENT_ID;

import generated.se.sundsvall.digitalregisteredletter.EligibilityRequest;
import generated.se.sundsvall.digitalregisteredletter.Letter;
import generated.se.sundsvall.digitalregisteredletter.LetterRequest;
import generated.se.sundsvall.digitalregisteredletter.Letters;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.postportalservice.integration.digitalregisteredletter.configuration.DigitalRegisteredLetterConfiguration;

@FeignClient(
	name = CLIENT_ID,
	url = "${integration.digitalregisteredletter.url}",
	configuration = DigitalRegisteredLetterConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface DigitalRegisteredLetterClient {

	@GetMapping(path = "/{municipalityId}/eligibility/kivra")
	List<String> checkKivraEligibility(
		@PathVariable final String municipalityId,
		@RequestParam final EligibilityRequest request);

	@GetMapping(path = "/{municipalityId}/letters")
	List<Letters> getAllLetters(@PathVariable final String municipalityId);

	@GetMapping(path = "/{municipalityId}/letters/{letterId}")
	Letter getLetterById(@PathVariable final String municipalityId,
		@PathVariable("letterId") final String letterId);

	@PostMapping(value = "/{municipalityId}/letters", consumes = MULTIPART_FORM_DATA)
	Letter sendLetter(
		@RequestHeader(Identifier.HEADER_NAME) String identifier,
		@PathVariable final String municipalityId,
		@RequestPart(name = "letter") final LetterRequest letterRequest,
		@RequestPart(name = "letterAttachments") final List<MultipartFile> files);

}
