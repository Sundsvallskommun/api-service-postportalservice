package se.sundsvall.postportalservice.integration.db;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.sql.Blob;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "attachment", indexes = {
	@Index(name = "IDX_ATTACHMENT_MESSAGE_ID", columnList = "message_id")
})
public class AttachmentEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", columnDefinition = "VARCHAR(36)")
	private String id;

	@Column(name = "file_name", columnDefinition = "VARCHAR(150)")
	private String fileName;

	@Column(name = "content_type", columnDefinition = "VARCHAR(50)")
	private String contentType;

	@Lob
	@Basic(fetch = FetchType.LAZY)
	@Column(name = "content", columnDefinition = "LONGBLOB")
	private Blob content;

	@Column(name = "created", columnDefinition = "DATETIME")
	private OffsetDateTime created;

	@PrePersist
	void prePersist() {
		created = OffsetDateTime.now();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public AttachmentEntity withId(String id) {
		this.id = id;
		return this;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public AttachmentEntity withFileName(String fileName) {
		this.fileName = fileName;
		return this;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public AttachmentEntity withContentType(String contentType) {
		this.contentType = contentType;
		return this;
	}

	public Blob getContent() {
		return content;
	}

	public void setContent(Blob content) {
		this.content = content;
	}

	public AttachmentEntity withContent(Blob content) {
		this.content = content;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	@Override
	public String toString() {
		return "AttachmentEntity{" +
			"id='" + id + '\'' +
			", fileName='" + fileName + '\'' +
			", contentType='" + contentType + '\'' +
			", content=" + content +
			", created=" + created +
			'}';
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		AttachmentEntity that = (AttachmentEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(fileName, that.fileName) && Objects.equals(contentType, that.contentType) && Objects.equals(content, that.content) && Objects.equals(created,
			that.created);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, fileName, contentType, content, created);
	}
}
