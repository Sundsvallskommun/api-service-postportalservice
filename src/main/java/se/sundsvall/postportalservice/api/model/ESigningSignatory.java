package se.sundsvall.postportalservice.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

@Schema(description = "A signatory that should sign the document")
public class ESigningSignatory {

	@Schema(description = "The party id of the signatory", examples = "6d0773d6-3e7f-4552-81bc-f0007af95adf")
	@ValidUuid
	private String partyId;

	@Schema(description = "The name of the signatory", examples = "John Doe")
	@NotBlank
	private String name;

	@Schema(description = "The email address of the signatory", examples = "john.doe@sundsvall.se")
	@Email
	@NotBlank
	private String email;

	public static ESigningSignatory create() {
		return new ESigningSignatory();
	}

	public String getPartyId() {
		return partyId;
	}

	public void setPartyId(String partyId) {
		this.partyId = partyId;
	}

	public ESigningSignatory withPartyId(String partyId) {
		this.partyId = partyId;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ESigningSignatory withName(String name) {
		this.name = name;
		return this;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public ESigningSignatory withEmail(String email) {
		this.email = email;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		ESigningSignatory that = (ESigningSignatory) o;
		return Objects.equals(partyId, that.partyId) && Objects.equals(name, that.name) && Objects.equals(email, that.email);
	}

	@Override
	public int hashCode() {
		return Objects.hash(partyId, name, email);
	}

	@Override
	public String toString() {
		return "ESigningSignatory{" +
			"partyId='" + partyId + '\'' +
			", name='" + name + '\'' +
			", email='" + email + '\'' +
			'}';
	}
}
