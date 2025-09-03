package se.sundsvall.postportalservice.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Objects;

@Schema(description = "Letter request model")
public class LetterRequest {

	@Schema(description = "The subject of the letter", example = "This is the subject of the letter")
	@NotBlank
	private String subject;

	@Schema(description = "The body of the letter", example = "This is the body of the letter")
	private String body;

	@Schema(description = "The content type of the body", example = "text/plain")
	private String contentType;

	@ArraySchema(schema = @Schema(description = "List of recipients", implementation = Recipient.class))
	@NotEmpty
	private List<@Valid Recipient> recipients;

	@ArraySchema(schema = @Schema(description = "List of addresses", implementation = Address.class))
	private List<@Valid Address> addresses;

	public static LetterRequest create() {
		return new LetterRequest();
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public LetterRequest withSubject(String subject) {
		this.subject = subject;
		return this;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public LetterRequest withBody(String body) {
		this.body = body;
		return this;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public LetterRequest withContentType(String contentType) {
		this.contentType = contentType;
		return this;
	}

	public List<Recipient> getRecipients() {
		return recipients;
	}

	public void setRecipients(List<Recipient> recipients) {
		this.recipients = recipients;
	}

	public LetterRequest withRecipients(List<Recipient> recipients) {
		this.recipients = recipients;
		return this;
	}

	public List<Address> getAddresses() {
		return addresses;
	}

	public void setAddresses(List<Address> addresses) {
		this.addresses = addresses;
	}

	public LetterRequest withAddresses(List<Address> addresses) {
		this.addresses = addresses;
		return this;
	}

	@Override
	public String toString() {
		return "LetterRequest{" +
			"subject='" + subject + '\'' +
			", body='" + body + '\'' +
			", contentType='" + contentType + '\'' +
			", recipients=" + recipients +
			", addresses=" + addresses +
			'}';
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		LetterRequest that = (LetterRequest) o;
		return Objects.equals(subject, that.subject) && Objects.equals(body, that.body) && Objects.equals(contentType, that.contentType) && Objects.equals(recipients, that.recipients) && Objects.equals(
			addresses, that.addresses);
	}

	@Override
	public int hashCode() {
		return Objects.hash(subject, body, contentType, recipients, addresses);
	}
}
