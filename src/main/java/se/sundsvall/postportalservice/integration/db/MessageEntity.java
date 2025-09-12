package se.sundsvall.postportalservice.integration.db;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import se.sundsvall.postportalservice.integration.db.converter.MessageType;

@Entity
@Table(name = "message", indexes = {
	@Index(name = "IDX_MESSAGE_DEPARTMENT_ID", columnList = "department_id"),
	@Index(name = "IDX_MESSAGE_USER_ID", columnList = "user_id"),
})
public class MessageEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", columnDefinition = "VARCHAR(36)")
	private String id;

	@Column(name = "municipality_id", columnDefinition = "VARCHAR(6)")
	private String municipalityId;

	@Column(name = "display_name", columnDefinition = "VARCHAR(100)")
	private String displayName;

	@Column(name = "message_type", columnDefinition = "VARCHAR(50)")
	private MessageType messageType;

	@Column(name = "text", columnDefinition = "TEXT")
	private String text;

	@Column(name = "created", columnDefinition = "DATETIME")
	private OffsetDateTime created;

	@ManyToOne(cascade = {
		CascadeType.MERGE, CascadeType.PERSIST
	})
	@JoinColumn(name = "user_id", columnDefinition = "VARCHAR(36)", foreignKey = @ForeignKey(name = "FK_MESSAGE_USER"))
	private UserEntity user;

	@ManyToOne(cascade = {
		CascadeType.MERGE, CascadeType.PERSIST
	})
	@JoinColumn(name = "department_id", columnDefinition = "VARCHAR(36)", foreignKey = @ForeignKey(name = "FK_MESSAGE_DEPARTMENT"))
	private DepartmentEntity department;

	@OneToMany(cascade = {
		CascadeType.MERGE, CascadeType.PERSIST
	}, orphanRemoval = true)
	@JoinColumn(name = "message_id", columnDefinition = "VARCHAR(36)", foreignKey = @ForeignKey(name = "FK_ATTACHMENT_MESSAGE"))
	private List<AttachmentEntity> attachments = new ArrayList<>();

	@OneToMany(cascade = {
		CascadeType.MERGE, CascadeType.PERSIST
	}, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "message_id", columnDefinition = "VARCHAR(36)", foreignKey = @ForeignKey(name = "FK_RECIPIENT_MESSAGE"))
	private List<RecipientEntity> recipients = new ArrayList<>();

	@PrePersist
	void prePersist() {
		created = OffsetDateTime.now();
	}

	public static MessageEntity create() {
		return new MessageEntity();
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public MessageEntity withDisplayName(String displayName) {
		this.displayName = displayName;
		return this;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public MessageEntity withId(String id) {
		this.id = id;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public MessageEntity withMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public MessageType getMessageType() {
		return messageType;
	}

	public void setMessageType(MessageType messageType) {
		this.messageType = messageType;
	}

	public MessageEntity withMessageType(MessageType messageType) {
		this.messageType = messageType;
		return this;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public MessageEntity withText(String text) {
		this.text = text;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	public MessageEntity withCreated(OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public UserEntity getUser() {
		return user;
	}

	public void setUser(UserEntity user) {
		this.user = user;
	}

	public MessageEntity withUser(UserEntity user) {
		this.user = user;
		return this;
	}

	public DepartmentEntity getDepartment() {
		return department;
	}

	public void setDepartment(DepartmentEntity department) {
		this.department = department;
	}

	public MessageEntity withDepartment(DepartmentEntity department) {
		this.department = department;
		return this;
	}

	public List<AttachmentEntity> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<AttachmentEntity> attachments) {
		this.attachments = attachments;
	}

	public MessageEntity withAttachments(List<AttachmentEntity> attachments) {
		this.attachments = attachments;
		return this;
	}

	public List<RecipientEntity> getRecipients() {
		return recipients;
	}

	public void setRecipients(List<RecipientEntity> recipients) {
		this.recipients = recipients;
	}

	public MessageEntity withRecipients(List<RecipientEntity> recipients) {
		this.recipients = recipients;
		return this;
	}

	@Override
	public String toString() {
		return "MessageEntity{" +
			"id='" + id + '\'' +
			", municipalityId='" + municipalityId + '\'' +
			", displayName='" + displayName + '\'' +
			", messageType=" + messageType +
			", text='" + text + '\'' +
			", created=" + created +
			", user=" + user +
			", department=" + department +
			", recipients=" + recipients +
			'}';
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		MessageEntity that = (MessageEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(municipalityId, that.municipalityId) && Objects.equals(displayName, that.displayName) && messageType == that.messageType && Objects.equals(text, that.text)
			&& Objects.equals(created, that.created) && Objects.equals(user, that.user) && Objects.equals(department, that.department) && Objects.equals(recipients, that.recipients);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, municipalityId, displayName, messageType, text, created, user, department, recipients);
	}
}
