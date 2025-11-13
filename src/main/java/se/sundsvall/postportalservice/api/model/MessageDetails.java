package se.sundsvall.postportalservice.api.model;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Schema(description = "Message details model")
public class MessageDetails {

	@Schema(description = "Message subject", accessMode = Schema.AccessMode.READ_ONLY, example = "This is a subject")
	private String subject;

	@Schema(description = "Message body", accessMode = Schema.AccessMode.READ_ONLY, example = "This is the message body")
	private String body;

	@Schema(description = "When the message was sent", accessMode = Schema.AccessMode.READ_ONLY, example = "2021-01-01T12:00:00")
	private LocalDateTime sentAt;

	@Schema(description = "Status for signing process. Only applicable for message type DIGITAL_REGISTERED_LETTER", requiredMode = NOT_REQUIRED, accessMode = Schema.AccessMode.READ_ONLY)
	private SigningStatus signingStatus;

	@ArraySchema(schema = @Schema(description = "List of attachment details", implementation = AttachmentDetails.class, accessMode = Schema.AccessMode.READ_ONLY))
	private List<AttachmentDetails> attachments;

	@ArraySchema(schema = @Schema(description = "List of recipient details", implementation = RecipientDetails.class, accessMode = Schema.AccessMode.READ_ONLY))
	private List<RecipientDetails> recipients;

	public static MessageDetails create() {
		return new MessageDetails();
	}

	public String getSubject() {
		return subject;
	}

	public MessageDetails withSubject(String subject) {
		this.subject = subject;
		return this;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public LocalDateTime getSentAt() {
		return sentAt;
	}

	public String getBody() {
		return body;
	}

	public MessageDetails withBody(String body) {
		this.body = body;
		return this;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public MessageDetails withSentAt(LocalDateTime sentAt) {
		this.sentAt = sentAt;
		return this;
	}

	public void setSentAt(LocalDateTime sentAt) {
		this.sentAt = sentAt;
	}

	public SigningStatus getSigningStatus() {
		return signingStatus;
	}

	public MessageDetails withSigningStatus(SigningStatus signingStatus) {
		this.signingStatus = signingStatus;
		return this;
	}

	public void setSigningStatus(SigningStatus signingStatus) {
		this.signingStatus = signingStatus;
	}

	public List<AttachmentDetails> getAttachments() {
		return attachments;
	}

	public MessageDetails withAttachments(List<AttachmentDetails> attachments) {
		this.attachments = attachments;
		return this;
	}

	public void setAttachments(List<AttachmentDetails> attachments) {
		this.attachments = attachments;
	}

	public List<RecipientDetails> getRecipients() {
		return recipients;
	}

	public MessageDetails withRecipients(List<RecipientDetails> recipients) {
		this.recipients = recipients;
		return this;
	}

	public void setRecipients(List<RecipientDetails> recipients) {
		this.recipients = recipients;
	}

	@Override
	public String toString() {
		return "MessageDetails{" +
			"subject='" + subject + '\'' +
			", body='" + body + '\'' +
			", sentAt=" + sentAt +
			", signingStatus=" + signingStatus +
			", attachments=" + attachments +
			", recipients=" + recipients +
			'}';
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		MessageDetails that = (MessageDetails) o;
		return Objects.equals(subject, that.subject) && Objects.equals(body, that.body) && Objects.equals(sentAt, that.sentAt) && Objects.equals(signingStatus, that.signingStatus) && Objects.equals(attachments, that.attachments) && Objects.equals(
			recipients, that.recipients);
	}

	@Override
	public int hashCode() {
		return Objects.hash(subject, body, sentAt, signingStatus, attachments, recipients);
	}

	public static class AttachmentDetails {

		@Schema(description = "Attachment ID", accessMode = Schema.AccessMode.READ_ONLY, example = "123e4567-e89b-12d3-a456-426614174000")
		private String attachmentId;

		@Schema(description = "File name of the attachment", accessMode = Schema.AccessMode.READ_ONLY, example = "document.pdf")
		private String fileName;

		@Schema(description = "MIME type of the attachment", accessMode = Schema.AccessMode.READ_ONLY, example = "application/pdf")
		private String contentType;

		public static AttachmentDetails create() {
			return new AttachmentDetails();
		}

		public String getAttachmentId() {
			return attachmentId;
		}

		public AttachmentDetails withAttachmentId(String attachmentId) {
			this.attachmentId = attachmentId;
			return this;
		}

		public void setAttachmentId(String attachmentId) {
			this.attachmentId = attachmentId;
		}

		public String getFileName() {
			return fileName;
		}

		public AttachmentDetails withFileName(String fileName) {
			this.fileName = fileName;
			return this;
		}

		public void setFileName(String fileName) {
			this.fileName = fileName;
		}

		public String getContentType() {
			return contentType;
		}

		public AttachmentDetails withContentType(String mimeType) {
			this.contentType = mimeType;
			return this;
		}

		public void setContentType(String contentType) {
			this.contentType = contentType;
		}

		@Override
		public String toString() {
			return "AttachmentDetails{" +
				"attachmentId='" + attachmentId + '\'' +
				", fileName='" + fileName + '\'' +
				", contentType='" + contentType + '\'' +
				'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass())
				return false;
			AttachmentDetails that = (AttachmentDetails) o;
			return Objects.equals(attachmentId, that.attachmentId) && Objects.equals(fileName, that.fileName) && Objects.equals(contentType, that.contentType);
		}

		@Override
		public int hashCode() {
			return Objects.hash(attachmentId, fileName, contentType);
		}
	}

	public static class RecipientDetails {

		@Schema(description = "Name of the recipient", accessMode = Schema.AccessMode.READ_ONLY, example = "John Doe")
		private String name;

		@Schema(description = "The recipients party ID", accessMode = Schema.AccessMode.READ_ONLY, example = "1234567890")
		private String partyId;

		@Schema(description = "Mobile number", accessMode = Schema.AccessMode.READ_ONLY, example = "+46701234567")
		private String mobileNumber;

		@Schema(description = "Street address", accessMode = Schema.AccessMode.READ_ONLY, example = "Main Street 5")
		private String streetAddress;

		@Schema(description = "Zip code", accessMode = Schema.AccessMode.READ_ONLY, example = "85751")
		private String zipCode;

		@Schema(description = "City", accessMode = Schema.AccessMode.READ_ONLY, example = "Sundsvall")
		private String city;

		@Schema(description = "Message type", accessMode = Schema.AccessMode.READ_ONLY, example = "SNAIL_MAIL|DIGITAL_MAIL|SMS")
		private String messageType;

		@Schema(description = "Status of the message to this recipient", accessMode = Schema.AccessMode.READ_ONLY, example = "SENT|NOT_SENT|FAILED")
		private String status;

		public static RecipientDetails create() {
			return new RecipientDetails();
		}

		public String getStatus() {
			return status;
		}

		public RecipientDetails withStatus(String status) {
			this.status = status;
			return this;
		}

		public void setStatus(String status) {
			this.status = status;
		}

		public String getMobileNumber() {
			return mobileNumber;
		}

		public RecipientDetails withMobileNumber(String mobileNumber) {
			this.mobileNumber = mobileNumber;
			return this;
		}

		public void setMobileNumber(String mobileNumber) {
			this.mobileNumber = mobileNumber;
		}

		public String getName() {
			return name;
		}

		public RecipientDetails withName(String name) {
			this.name = name;
			return this;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getPartyId() {
			return partyId;
		}

		public RecipientDetails withPartyId(String partyId) {
			this.partyId = partyId;
			return this;
		}

		public void setPartyId(String partyId) {
			this.partyId = partyId;
		}

		public String getStreetAddress() {
			return streetAddress;
		}

		public RecipientDetails withStreetAddress(String streetAddress) {
			this.streetAddress = streetAddress;
			return this;
		}

		public void setStreetAddress(String streetAddress) {
			this.streetAddress = streetAddress;
		}

		public String getZipCode() {
			return zipCode;
		}

		public RecipientDetails withZipCode(String zipCode) {
			this.zipCode = zipCode;
			return this;
		}

		public void setZipCode(String zipCode) {
			this.zipCode = zipCode;
		}

		public String getCity() {
			return city;
		}

		public RecipientDetails withCity(String city) {
			this.city = city;
			return this;
		}

		public void setCity(String city) {
			this.city = city;
		}

		public String getMessageType() {
			return messageType;
		}

		public RecipientDetails withMessageType(String messageType) {
			this.messageType = messageType;
			return this;
		}

		public void setMessageType(String messageType) {
			this.messageType = messageType;
		}

		@Override
		public String toString() {
			return "RecipientDetails{" +
				"name='" + name + '\'' +
				", partyId='" + partyId + '\'' +
				", mobileNumber='" + mobileNumber + '\'' +
				", streetAddress='" + streetAddress + '\'' +
				", zipCode='" + zipCode + '\'' +
				", city='" + city + '\'' +
				", messageType='" + messageType + '\'' +
				", status='" + status + '\'' +
				'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass())
				return false;
			RecipientDetails that = (RecipientDetails) o;
			return Objects.equals(name, that.name) && Objects.equals(partyId, that.partyId) && Objects.equals(mobileNumber, that.mobileNumber) && Objects.equals(streetAddress, that.streetAddress) && Objects.equals(
				zipCode, that.zipCode) && Objects.equals(city, that.city) && Objects.equals(messageType, that.messageType) && Objects.equals(status, that.status);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, partyId, mobileNumber, streetAddress, zipCode, city, messageType, status);
		}
	}

}
