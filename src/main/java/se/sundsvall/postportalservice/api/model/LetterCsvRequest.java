package se.sundsvall.postportalservice.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;

@Schema(description = "Letter CSV request model")
public class LetterCsvRequest {

	@Schema(description = "The subject of the letter", examples = "This is the subject of the letter")
	@NotBlank
	private String subject;

	@Schema(description = "The body of the letter", examples = "This is the body of the letter")
	@NotBlank
	private String body;

	@Schema(description = "The content type of the body", examples = "text/plain")
	@NotBlank
	private String contentType;

	public static LetterCsvRequest create() {
		return new LetterCsvRequest();
	}

	public String getSubject() {
		return subject;
	}

	public LetterCsvRequest withSubject(String subject) {
		this.subject = subject;
		return this;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getBody() {
		return body;
	}

	public LetterCsvRequest withBody(String body) {
		this.body = body;
		return this;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getContentType() {
		return contentType;
	}

	public LetterCsvRequest withContentType(String contentType) {
		this.contentType = contentType;
		return this;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	@Override
	public String toString() {
		return "LetterCsvRequest{" +
			"subject='" + subject + '\'' +
			", body='" + body + '\'' +
			", contentType='" + contentType + '\'' +
			'}';
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		LetterCsvRequest that = (LetterCsvRequest) o;
		return Objects.equals(subject, that.subject) && Objects.equals(body, that.body) && Objects.equals(contentType, that.contentType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(subject, body, contentType);
	}
}
