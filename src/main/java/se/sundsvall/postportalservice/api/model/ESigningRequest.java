package se.sundsvall.postportalservice.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@Schema(description = "E-signing request model")
public class ESigningRequest {

	@Schema(description = "The subject of the notification sent to the signatories", examples = "Please sign the document")
	@NotBlank
	private String subject;

	@Schema(description = "The body of the notification sent to the signatories", examples = "Dear John Doe, please sign the attached document.")
	@NotBlank
	private String body;

	@Schema(description = "The language used for the signing instance. Swedish is used if not provided", examples = "sv-SE")
	private String language;

	@Schema(description = "Optional date and time when the signing request expires", examples = "2026-12-31T23:59:59Z")
	@Future
	private OffsetDateTime expires;

	@ArraySchema(schema = @Schema(implementation = ESigningSignatory.class), minItems = 1)
	@NotEmpty
	private List<@Valid ESigningSignatory> signatories;

	public static ESigningRequest create() {
		return new ESigningRequest();
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public ESigningRequest withSubject(String subject) {
		this.subject = subject;
		return this;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public ESigningRequest withBody(String body) {
		this.body = body;
		return this;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public ESigningRequest withLanguage(String language) {
		this.language = language;
		return this;
	}

	public OffsetDateTime getExpires() {
		return expires;
	}

	public void setExpires(OffsetDateTime expires) {
		this.expires = expires;
	}

	public ESigningRequest withExpires(OffsetDateTime expires) {
		this.expires = expires;
		return this;
	}

	public List<ESigningSignatory> getSignatories() {
		return signatories;
	}

	public void setSignatories(List<ESigningSignatory> signatories) {
		this.signatories = signatories;
	}

	public ESigningRequest withSignatories(List<ESigningSignatory> signatories) {
		this.signatories = signatories;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		ESigningRequest that = (ESigningRequest) o;
		return Objects.equals(subject, that.subject) && Objects.equals(body, that.body) && Objects.equals(language, that.language) && Objects.equals(expires, that.expires) && Objects.equals(signatories, that.signatories);
	}

	@Override
	public int hashCode() {
		return Objects.hash(subject, body, language, expires, signatories);
	}

	@Override
	public String toString() {
		return "ESigningRequest{" +
			"subject='" + subject + '\'' +
			", body='" + body + '\'' +
			", language='" + language + '\'' +
			", expires=" + expires +
			", signatories=" + signatories +
			'}';
	}
}
