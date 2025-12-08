package se.sundsvall.postportalservice.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Objects;
import org.hibernate.validator.constraints.UniqueElements;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

@Schema(description = "Kivra eligibility request model")
public class KivraEligibilityRequest {

	@Schema(description = "List of party IDs to check for Kivra eligibility", examples = "[\"da03b33e-9de2-45ac-8291-31a88de59410\", \"eb13b33e-9de2-45ac-8291-31a88de59411\"]")
	@UniqueElements
	@NotEmpty
	private List<@ValidUuid String> partyIds;

	public static KivraEligibilityRequest create() {
		return new KivraEligibilityRequest();
	}

	public List<String> getPartyIds() {
		return partyIds;
	}

	public KivraEligibilityRequest withPartyIds(List<String> partyIds) {
		this.partyIds = partyIds;
		return this;
	}

	public void setPartyIds(List<String> partyIds) {
		this.partyIds = partyIds;
	}

	@Override
	public String toString() {
		return "KivraEligibilityRequest{" +
			"partyIds=" + partyIds +
			'}';
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		KivraEligibilityRequest that = (KivraEligibilityRequest) o;
		return Objects.equals(partyIds, that.partyIds);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(partyIds);
	}
}
