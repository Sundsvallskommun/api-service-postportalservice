package se.sundsvall.postportalservice.api.model;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "Signing status model")
public class SigningStatus {

	@Schema(description = "Present state for the letter", examples = {
		"NEW", "SENT", "SIGNED", "EXPIRED", "FAILED - Client Error", "FAILED - Server Error", "FAILED - Unknown Error"
	}, accessMode = READ_ONLY)
	private String letterState;

	@Schema(description = "Present state for the signing process", examples = {
		"PENDING", "COMPLETED", "FAILED"
	}, accessMode = READ_ONLY)
	private String signingProcessState;

	public static SigningStatus create() {
		return new SigningStatus();
	}

	public String getLetterState() {
		return letterState;
	}

	public void setLetterState(final String letterState) {
		this.letterState = letterState;
	}

	public SigningStatus withLetterState(final String letterState) {
		this.letterState = letterState;
		return this;
	}

	public String getSigningProcessState() {
		return signingProcessState;
	}

	public void setSigningProcessState(final String signingProcessState) {
		this.signingProcessState = signingProcessState;
	}

	public SigningStatus withSigningProcessState(final String signingProcessState) {
		this.signingProcessState = signingProcessState;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(letterState, signingProcessState);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final SigningStatus other)) {
			return false;
		}
		return Objects.equals(letterState, other.letterState) && Objects.equals(signingProcessState, other.signingProcessState);
	}

	@Override
	public String toString() {
		return "SigningStatus{" +
			"letterState='" + letterState + '\'' +
			", signingProcessState='" + signingProcessState + '\'' +
			'}';
	}
}
