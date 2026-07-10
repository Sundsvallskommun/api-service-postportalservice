package se.sundsvall.postportalservice.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.OneOf;

@Schema(description = "A provider-neutral signing event delivered by api-service-e-signing")
public class SigningEvent {

	@Schema(description = "The consumer's own reference echoed back by the provider (the Postportalen message id)", examples = "550e8400-e29b-41d4-a716-446655440000")
	private String customerReference;

	@Schema(description = "The signing provider's case id", examples = "1234567890")
	@NotBlank
	private String providerCaseId;

	@Schema(description = "The id of the signing provider that produced the event", examples = "comfact")
	private String provider;

	@OneOf(value = {
		"CASE_CREATED", "SIGNATORY_APPROVED", "SIGNATORY_DECLINED", "CASE_COMPLETED",
		"CASE_WITHDRAWN", "CASE_EXPIRED", "CASE_HALTED", "CASE_REACTIVATED"
	}, message = "The provided event type is not a known signing event type.")
	@Schema(description = "The normalized event type", examples = "CASE_COMPLETED", allowableValues = {
		"CASE_CREATED", "SIGNATORY_APPROVED", "SIGNATORY_DECLINED", "CASE_COMPLETED", "CASE_WITHDRAWN", "CASE_EXPIRED", "CASE_HALTED", "CASE_REACTIVATED"
	})
	private String eventType;

	@OneOf(value = {
		"INITIATED", "PENDING", "SIGNED", "EXPIRED", "FAILED"
	}, message = "The provided status is not a known signing status.")
	@Schema(description = "The normalized case status", examples = "SIGNED", allowableValues = {
		"INITIATED", "PENDING", "SIGNED", "EXPIRED", "FAILED"
	})
	private String status;

	@Valid
	@Schema(description = "The acting signatory, present on signatory events", implementation = EventSignatory.class)
	private EventSignatory signatory;

	@Valid
	@Schema(description = "The signed document, present only on a completed event", implementation = SignedDocument.class)
	private SignedDocument signedDocument;

	@Schema(description = "When the event occurred at the provider", examples = "2026-12-31T23:59:59Z")
	private OffsetDateTime occurredAt;

	public static SigningEvent create() {
		return new SigningEvent();
	}

	public String getCustomerReference() {
		return customerReference;
	}

	public void setCustomerReference(String customerReference) {
		this.customerReference = customerReference;
	}

	public SigningEvent withCustomerReference(String customerReference) {
		this.customerReference = customerReference;
		return this;
	}

	public String getProviderCaseId() {
		return providerCaseId;
	}

	public void setProviderCaseId(String providerCaseId) {
		this.providerCaseId = providerCaseId;
	}

	public SigningEvent withProviderCaseId(String providerCaseId) {
		this.providerCaseId = providerCaseId;
		return this;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public SigningEvent withProvider(String provider) {
		this.provider = provider;
		return this;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public SigningEvent withEventType(String eventType) {
		this.eventType = eventType;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public SigningEvent withStatus(String status) {
		this.status = status;
		return this;
	}

	public EventSignatory getSignatory() {
		return signatory;
	}

	public void setSignatory(EventSignatory signatory) {
		this.signatory = signatory;
	}

	public SigningEvent withSignatory(EventSignatory signatory) {
		this.signatory = signatory;
		return this;
	}

	public SignedDocument getSignedDocument() {
		return signedDocument;
	}

	public void setSignedDocument(SignedDocument signedDocument) {
		this.signedDocument = signedDocument;
	}

	public SigningEvent withSignedDocument(SignedDocument signedDocument) {
		this.signedDocument = signedDocument;
		return this;
	}

	public OffsetDateTime getOccurredAt() {
		return occurredAt;
	}

	public void setOccurredAt(OffsetDateTime occurredAt) {
		this.occurredAt = occurredAt;
	}

	public SigningEvent withOccurredAt(OffsetDateTime occurredAt) {
		this.occurredAt = occurredAt;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		SigningEvent that = (SigningEvent) o;
		return Objects.equals(customerReference, that.customerReference) && Objects.equals(providerCaseId, that.providerCaseId) && Objects.equals(provider, that.provider) && Objects.equals(eventType, that.eventType)
			&& Objects.equals(status, that.status) && Objects.equals(signatory, that.signatory) && Objects.equals(signedDocument, that.signedDocument) && Objects.equals(occurredAt, that.occurredAt);
	}

	@Override
	public int hashCode() {
		return Objects.hash(customerReference, providerCaseId, provider, eventType, status, signatory, signedDocument, occurredAt);
	}

	@Override
	public String toString() {
		return "SigningEvent{" +
			"customerReference='" + customerReference + '\'' +
			", providerCaseId='" + providerCaseId + '\'' +
			", provider='" + provider + '\'' +
			", eventType='" + eventType + '\'' +
			", status='" + status + '\'' +
			", signatory=" + signatory +
			", signedDocument=" + signedDocument +
			", occurredAt=" + occurredAt +
			'}';
	}
}
