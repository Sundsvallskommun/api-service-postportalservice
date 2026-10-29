package se.sundsvall.postportalservice.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Forward document request model")
public class ForwardDocumentRequest {

	@Schema(description = "The subject of the letter", examples = "This is the subject of the letter", requiredMode = REQUIRED)
	@NotBlank
	private String subject;

	@Schema(description = "The body of the letter", examples = "This is the body of the letter", requiredMode = NOT_REQUIRED)
	private String body;

	@Schema(description = "The content type of the body", examples = "text/plain", requiredMode = NOT_REQUIRED)
	private String contentType;

	@Schema(description = "List of party ids of the recipients", examples = "[\"6d0773d6-3e7f-4552-81bc-f0007af95adf\"]")
	@NotEmpty
	private List<@ValidUuid String> recipientPartyIds;

	public static ForwardDocumentRequest create() {
		return new ForwardDocumentRequest();
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public ForwardDocumentRequest withSubject(String subject) {
		this.subject = subject;
		return this;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public ForwardDocumentRequest withBody(String body) {
		this.body = body;
		return this;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public ForwardDocumentRequest withContentType(String contentType) {
		this.contentType = contentType;
		return this;
	}

	public List<String> getRecipientPartyIds() {
		return recipientPartyIds;
	}

	public void setRecipientPartyIds(List<String> recipientPartyIds) {
		this.recipientPartyIds = recipientPartyIds;
	}

	public ForwardDocumentRequest withRecipientPartyIds(List<String> recipientPartyIds) {
		this.recipientPartyIds = recipientPartyIds;
		return this;
	}

	@Override
	public String toString() {
		return "ForwardDocumentRequest{" +
			"subject='" + subject + '\'' +
			", body='" + body + '\'' +
			", contentType='" + contentType + '\'' +
			", recipientPartyIds=" + recipientPartyIds +
			'}';
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		ForwardDocumentRequest that = (ForwardDocumentRequest) o;
		return Objects.equals(subject, that.subject) && Objects.equals(body, that.body) && Objects.equals(contentType, that.contentType) && Objects.equals(recipientPartyIds, that.recipientPartyIds);
	}

	@Override
	public int hashCode() {
		return Objects.hash(subject, body, contentType, recipientPartyIds);
	}
}
