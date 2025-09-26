package se.sundsvall.postportalservice.api.model;

import static org.springframework.http.MediaType.TEXT_HTML_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.OneOf;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

@Schema(description = "Digital registered letter request model")
public class DigitalRegisteredLetterRequest {

	@Schema(description = "The body of the letter in HTML format", example = "<h1>This is the body of the letter</h1>")
	@NotBlank
	private String body;

	@Schema(description = "The content type of the body", example = "text/html")
	@OneOf({
		TEXT_PLAIN_VALUE, TEXT_HTML_VALUE
	})
	private String contentType;

	@Schema(description = "The party id of the recipient", example = "6d0773d6-3e7f-4552-81bc-f0007af95adf")
	@ValidUuid
	private String partyId;

	@Schema(description = "The subject of the letter", example = "This is the subject of the letter")
	@NotBlank
	private String subject;

	public static DigitalRegisteredLetterRequest create() {
		return new DigitalRegisteredLetterRequest();
	}

	public String getBody() {
		return body;
	}

	public DigitalRegisteredLetterRequest withBody(String body) {
		this.body = body;
		return this;
	}

	public String getContentType() {
		return contentType;
	}

	public DigitalRegisteredLetterRequest withContentType(String contentType) {
		this.contentType = contentType;
		return this;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getPartyId() {
		return partyId;
	}

	public void setPartyId(String partyId) {
		this.partyId = partyId;
	}

	public DigitalRegisteredLetterRequest withPartyId(String partyId) {
		this.partyId = partyId;
		return this;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public DigitalRegisteredLetterRequest withSubject(String subject) {
		this.subject = subject;
		return this;
	}

	@Override
	public String toString() {
		return "DigitalRegisteredLetterRequest{" +
			"body='" + body + '\'' +
			", contentType='" + contentType + '\'' +
			", partyId='" + partyId + '\'' +
			", subject='" + subject + '\'' +
			'}';
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		DigitalRegisteredLetterRequest that = (DigitalRegisteredLetterRequest) o;
		return Objects.equals(body, that.body) && Objects.equals(contentType, that.contentType) && Objects.equals(partyId, that.partyId) && Objects.equals(subject, that.subject);
	}

	@Override
	public int hashCode() {
		return Objects.hash(body, contentType, partyId, subject);
	}
}
