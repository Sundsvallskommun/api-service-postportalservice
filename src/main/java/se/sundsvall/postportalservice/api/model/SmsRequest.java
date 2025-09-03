package se.sundsvall.postportalservice.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Objects;

@Schema(description = "SMS request model")
public class SmsRequest {

	@Schema(description = "The message to be sent", example = "This is the message to be sent")
	@NotBlank
	private String message;

	@ArraySchema(schema = @Schema(description = "List of recipients", implementation = SmsRecipient.class))
	@NotEmpty
	private List<@Valid SmsRecipient> recipients;

	public static SmsRequest create() {
		return new SmsRequest();
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public SmsRequest withMessage(String message) {
		this.message = message;
		return this;
	}

	public List<SmsRecipient> getRecipients() {
		return recipients;
	}

	public void setRecipients(List<SmsRecipient> recipients) {
		this.recipients = recipients;
	}

	public SmsRequest withRecipients(List<SmsRecipient> recipients) {
		this.recipients = recipients;
		return this;
	}

	@Override
	public String toString() {
		return "SmsRequest{" +
			"message='" + message + '\'' +
			", recipients=" + recipients +
			'}';
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		SmsRequest that = (SmsRequest) o;
		return Objects.equals(message, that.message) && Objects.equals(recipients, that.recipients);
	}

	@Override
	public int hashCode() {
		return Objects.hash(message, recipients);
	}
}
