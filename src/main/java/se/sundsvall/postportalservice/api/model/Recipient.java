package se.sundsvall.postportalservice.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.postportalservice.api.validation.ValidRecipient;
import se.sundsvall.postportalservice.api.validation.groups.SnailMailGroup;

@Schema(description = "Recipient model")
@ValidRecipient
public class Recipient {

	@Schema(description = "PartyId is the unique identifier for the recipient", example = "6d0773d6-3e7f-4552-81bc-f0007af95adf")
	@ValidUuid
	private String partyId;

	@Schema(description = "Delivery method for the recipient", example = "DIGITAL_MAIL", implementation = DeliveryMethod.class)
	@NotNull
	private DeliveryMethod deliveryMethod;

	@Schema(description = "Address details for the recipient, used for SNAIL_MAIL delivery method", implementation = Address.class)
	@Valid
	@NotNull(groups = SnailMailGroup.class)
	private Address address;

	public enum DeliveryMethod {
		DIGITAL_MAIL, SNAIL_MAIL, DELIVERY_NOT_POSSIBLE
	}

	public static Recipient create() {
		return new Recipient();
	}

	public String getPartyId() {
		return partyId;
	}

	public void setPartyId(String partyId) {
		this.partyId = partyId;
	}

	public Recipient withPartyId(String partyId) {
		this.partyId = partyId;
		return this;

	}

	public DeliveryMethod getDeliveryMethod() {
		return deliveryMethod;
	}

	public void setDeliveryMethod(DeliveryMethod deliveryMethod) {
		this.deliveryMethod = deliveryMethod;
	}

	public Recipient withDeliveryMethod(DeliveryMethod deliveryMethod) {
		this.deliveryMethod = deliveryMethod;
		return this;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public Recipient withAddress(Address address) {
		this.address = address;
		return this;
	}

	@Override
	public String toString() {
		return "Recipient{" +
			"partyId='" + partyId + '\'' +
			", deliveryMethod=" + deliveryMethod +
			", address=" + address +
			'}';
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		Recipient recipient = (Recipient) o;
		return Objects.equals(partyId, recipient.partyId) && deliveryMethod == recipient.deliveryMethod && Objects.equals(address, recipient.address);
	}

	@Override
	public int hashCode() {
		return Objects.hash(partyId, deliveryMethod, address);
	}
}
