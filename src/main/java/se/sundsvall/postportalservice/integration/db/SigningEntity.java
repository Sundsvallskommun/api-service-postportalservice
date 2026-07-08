package se.sundsvall.postportalservice.integration.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;

import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

/**
 * Case-level record for an e-signing (message type {@code E_SIGNING}). Holds the provider correlation
 * ({@code providerCaseId}, {@code provider}) and the normalized case {@code status}, and points at the
 * {@code attachment} that holds the document being signed (overwritten in place with the signed PDF on completion).
 * It owns its {@code message_id} and {@code attachment_id} references, so {@link MessageEntity} and
 * {@link RecipientEntity} stay untouched. Per-signatory status lives on the message's recipients, keyed by party id.
 */
@Entity
@Table(name = "signing", indexes = {
	@Index(name = "IDX_SIGNING_MESSAGE_ID", columnList = "message_id"),
	@Index(name = "IDX_SIGNING_PROVIDER_CASE_ID", columnList = "provider_case_id")
})
public class SigningEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", columnDefinition = "VARCHAR(36)")
	private String id;

	@Column(name = "message_id", columnDefinition = "VARCHAR(36)")
	private String messageId;

	@Column(name = "provider_case_id", columnDefinition = "VARCHAR(255)")
	private String providerCaseId;

	@Column(name = "provider", columnDefinition = "VARCHAR(50)")
	private String provider;

	@Column(name = "status", columnDefinition = "VARCHAR(50)")
	private String status;

	@Column(name = "attachment_id", columnDefinition = "VARCHAR(36)")
	private String attachmentId;

	@Column(name = "created", columnDefinition = "DATETIME")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@PrePersist
	void prePersist() {
		created = OffsetDateTime.now(ZoneId.systemDefault());
	}

	public static SigningEntity create() {
		return new SigningEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public SigningEntity withId(String id) {
		this.id = id;
		return this;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public SigningEntity withMessageId(String messageId) {
		this.messageId = messageId;
		return this;
	}

	public String getProviderCaseId() {
		return providerCaseId;
	}

	public void setProviderCaseId(String providerCaseId) {
		this.providerCaseId = providerCaseId;
	}

	public SigningEntity withProviderCaseId(String providerCaseId) {
		this.providerCaseId = providerCaseId;
		return this;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public SigningEntity withProvider(String provider) {
		this.provider = provider;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public SigningEntity withStatus(String status) {
		this.status = status;
		return this;
	}

	public String getAttachmentId() {
		return attachmentId;
	}

	public void setAttachmentId(String attachmentId) {
		this.attachmentId = attachmentId;
	}

	public SigningEntity withAttachmentId(String attachmentId) {
		this.attachmentId = attachmentId;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	public SigningEntity withCreated(OffsetDateTime created) {
		this.created = created;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		SigningEntity that = (SigningEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(messageId, that.messageId) && Objects.equals(providerCaseId, that.providerCaseId) && Objects.equals(provider, that.provider) && Objects.equals(status, that.status)
			&& Objects.equals(attachmentId, that.attachmentId) && Objects.equals(created, that.created);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, messageId, providerCaseId, provider, status, attachmentId, created);
	}

	@Override
	public String toString() {
		return "SigningEntity{" +
			"id='" + id + '\'' +
			", messageId='" + messageId + '\'' +
			", providerCaseId='" + providerCaseId + '\'' +
			", provider='" + provider + '\'' +
			", status='" + status + '\'' +
			", attachmentId='" + attachmentId + '\'' +
			", created=" + created +
			'}';
	}
}
