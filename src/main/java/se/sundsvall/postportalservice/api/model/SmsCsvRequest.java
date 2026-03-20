package se.sundsvall.postportalservice.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;

@Schema(description = "SMS CSV request model")
public class SmsCsvRequest {

	@Schema(description = "The message to be sent", examples = "This is the message to be sent")
	@NotBlank
	private String message;

	public static SmsCsvRequest create() {
		return new SmsCsvRequest();
	}

	public String getMessage() {
		return message;
	}

	public SmsCsvRequest withMessage(String message) {
		this.message = message;
		return this;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return "SmsCsvRequest{" +
			"message='" + message + '\'' +
			'}';
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		SmsCsvRequest that = (SmsCsvRequest) o;
		return Objects.equals(message, that.message);
	}

	@Override
	public int hashCode() {
		return Objects.hash(message);
	}
}
