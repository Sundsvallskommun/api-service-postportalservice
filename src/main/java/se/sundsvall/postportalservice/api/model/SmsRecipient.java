package se.sundsvall.postportalservice.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidMSISDN;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

@Schema(description = "SMS recipient model")
public class SmsRecipient {

	@Schema(description = "PartyId is the unique identifier for the recipient", example = "6d0773d6-3e7f-4552-81bc-f0007af95adf")
	@ValidUuid
	private String partyId;

	@Schema(description = "Phone number of the recipient, used for SMS notifications", example = "+46701234567")
	@ValidMSISDN
	private String phoneNumber;

	public static SmsRecipient create() {
		return new SmsRecipient();
	}

	public String getPartyId() {
		return partyId;
	}

	public void setPartyId(String partyId) {
		this.partyId = partyId;
	}

	public SmsRecipient withPartyId(String partyId) {
		this.partyId = partyId;
		return this;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public SmsRecipient withPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
		return this;
	}

	@Override
	public String toString() {
		return "SmsRecipient{" +
			"partyId='" + partyId + '\'' +
			", phoneNumber='" + phoneNumber + '\'' +
			'}';
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		SmsRecipient that = (SmsRecipient) o;
		return Objects.equals(partyId, that.partyId) && Objects.equals(phoneNumber, that.phoneNumber);
	}

	@Override
	public int hashCode() {
		return Objects.hash(partyId, phoneNumber);
	}
}
