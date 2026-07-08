package se.sundsvall.postportalservice.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.OneOf;

@Schema(description = "The signatory that acted in a signing event")
public class EventSignatory {

	@Schema(description = "The party id of the signatory", examples = "6d0773d6-3e7f-4552-81bc-f0007af95adf")
	private String partyId;

	@OneOf(value = {
		"APPROVED", "DECLINED"
	}, message = "The provided action is not a known signatory action.", nullable = true)
	@Schema(description = "The normalized action taken by the signatory", examples = "APPROVED", allowableValues = {
		"APPROVED", "DECLINED"
	})
	private String action;

	@Schema(description = "The reason given for the action, when provided", examples = "Not authorised to sign")
	private String reason;

	public static EventSignatory create() {
		return new EventSignatory();
	}

	public String getPartyId() {
		return partyId;
	}

	public void setPartyId(String partyId) {
		this.partyId = partyId;
	}

	public EventSignatory withPartyId(String partyId) {
		this.partyId = partyId;
		return this;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public EventSignatory withAction(String action) {
		this.action = action;
		return this;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public EventSignatory withReason(String reason) {
		this.reason = reason;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		EventSignatory that = (EventSignatory) o;
		return Objects.equals(partyId, that.partyId) && Objects.equals(action, that.action) && Objects.equals(reason, that.reason);
	}

	@Override
	public int hashCode() {
		return Objects.hash(partyId, action, reason);
	}

	@Override
	public String toString() {
		return "EventSignatory{" +
			"partyId='" + partyId + '\'' +
			", action='" + action + '\'' +
			", reason='" + reason + '\'' +
			'}';
	}
}
