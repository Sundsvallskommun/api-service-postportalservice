package se.sundsvall.postportalservice.integration.db;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.TimeZoneStorage;

import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

/**
 * Case-level record for an e-signing (message type {@code E_SIGNING}). Owns the foreign key to its
 * {@link MessageEntity} ({@code message_id}); look the case up from a message via
 * {@code SigningRepository.findByMessageId(..)}. Once the case is signed it also references the
 * {@link AttachmentEntity}
 * holding the signed document (the merged signed PDF Comfact returns; {@code null} until completion). Per-signatory
 * status lives on the message's recipients, keyed by party id.
 */
@Entity
@Table(name = "signing")
public class SigningEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", columnDefinition = "VARCHAR(36)")
	private String id;

	// A signing case is subordinate to its message, so a (future) message delete cascades the signing row away rather
	// than being blocked by this FK. Attachment side is intentionally left plain (matches the sibling attachment FKs;
	// the signed document has no independent delete path).
	@OneToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "message_id", columnDefinition = "VARCHAR(36)", foreignKey = @ForeignKey(name = "FK_SIGNING_MESSAGE"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private MessageEntity message;

	@Column(name = "provider_case_id", columnDefinition = "VARCHAR(255)")
	private String providerCaseId;

	@Column(name = "provider", columnDefinition = "VARCHAR(50)")
	private String provider;

	@Column(name = "status", columnDefinition = "VARCHAR(50)")
	private String status;

	// Eager: a nullable owning @OneToOne can't be lazily proxied without bytecode enhancement. Negligible - a signing is
	// only ever loaded one at a time.
	@OneToOne(cascade = {
		CascadeType.MERGE, CascadeType.PERSIST
	})
	@JoinColumn(name = "attachment_id", columnDefinition = "VARCHAR(36)", foreignKey = @ForeignKey(name = "FK_SIGNING_ATTACHMENT"))
	private AttachmentEntity attachment;

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

	public MessageEntity getMessage() {
		return message;
	}

	public void setMessage(MessageEntity message) {
		this.message = message;
	}

	public SigningEntity withMessage(MessageEntity message) {
		this.message = message;
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

	public AttachmentEntity getAttachment() {
		return attachment;
	}

	public void setAttachment(AttachmentEntity attachment) {
		this.attachment = attachment;
	}

	public SigningEntity withAttachment(AttachmentEntity attachment) {
		this.attachment = attachment;
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

	// message and attachment are intentionally excluded from equals/hashCode/toString to avoid bidirectional recursion
	// and touching lazy proxies.
	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		SigningEntity that = (SigningEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(providerCaseId, that.providerCaseId) && Objects.equals(provider, that.provider) && Objects.equals(status, that.status) && Objects.equals(created, that.created);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, providerCaseId, provider, status, created);
	}

	@Override
	public String toString() {
		return "SigningEntity{" +
			"id='" + id + '\'' +
			", providerCaseId='" + providerCaseId + '\'' +
			", provider='" + provider + '\'' +
			", status='" + status + '\'' +
			", created=" + created +
			'}';
	}
}
