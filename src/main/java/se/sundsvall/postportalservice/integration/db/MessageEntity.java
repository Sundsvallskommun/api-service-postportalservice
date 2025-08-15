package se.sundsvall.postportalservice.integration.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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

	@Column(name = "messaging_id", columnDefinition = "VARCHAR(36)")
	private String messagingId;

	@Column(name = "original_message_type", columnDefinition = "VARCHAR(50)")
	private MessageType originalMessageType;

	@Column(name = "text", columnDefinition = "TEXT")
	private String text;

	@ManyToOne
	@JoinColumn(name = "user_id", columnDefinition = "VARCHAR(36)")
	private UserEntity user;

	@ManyToOne
	@JoinColumn(name = "department_id", columnDefinition = "VARCHAR(36)")
	private DepartmentEntity department;

	@OneToMany
	@JoinColumn(name = "message_id", columnDefinition = "VARCHAR(36) NOT NULL")
	private List<AttachmentEntity> attachments;

	@OneToMany
	@JoinColumn(name = "message_id", columnDefinition = "VARCHAR(36) NOT NULL")
	private List<RecipientEntity> recipients;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getMessagingId() {
		return messagingId;
	}

	public void setMessagingId(String messagingId) {
		this.messagingId = messagingId;
	}

	public MessageType getOriginalMessageType() {
		return originalMessageType;
	}

	public void setOriginalMessageType(MessageType originalMessageType) {
		this.originalMessageType = originalMessageType;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public UserEntity getUser() {
		return user;
	}

	public void setUser(UserEntity user) {
		this.user = user;
	}

	public DepartmentEntity getDepartment() {
		return department;
	}

	public void setDepartment(DepartmentEntity department) {
		this.department = department;
	}

	public List<AttachmentEntity> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<AttachmentEntity> attachments) {
		this.attachments = attachments;
	}

	public List<RecipientEntity> getRecipients() {
		return recipients;
	}

	public void setRecipients(List<RecipientEntity> recipients) {
		this.recipients = recipients;
	}

	@Override
	public String toString() {
		return "MessageEntity{" +
			"id='" + id + '\'' +
			", messagingId='" + messagingId + '\'' +
			", originalMessageType=" + originalMessageType +
			", text='" + text + '\'' +
			", user=" + user +
			", department=" + department +
			", attachments=" + attachments +
			", recipients=" + recipients +
			'}';
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		MessageEntity that = (MessageEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(messagingId, that.messagingId) && originalMessageType == that.originalMessageType && Objects.equals(text, that.text) && Objects.equals(user, that.user)
			&& Objects.equals(department, that.department) && Objects.equals(attachments, that.attachments) && Objects.equals(recipients, that.recipients);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, messagingId, originalMessageType, text, user, department, attachments, recipients);
	}
}
