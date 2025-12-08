package se.sundsvall.postportalservice.api.model;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Schema(description = "Message details model")
public class MessageDetails {

	@Schema(description = "Message subject", accessMode = Schema.AccessMode.READ_ONLY, examples = "This is a subject")
	private String subject;

	@Schema(description = "Message body", accessMode = Schema.AccessMode.READ_ONLY, examples = "This is the message body")
	private String body;

	@Schema(description = "When the message was sent", accessMode = Schema.AccessMode.READ_ONLY, examples = "2021-01-01T12:00:00")
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

	public void setSubject(final String subject) {
		this.subject = subject;
	}

	public MessageDetails withSubject(final String subject) {
		this.subject = subject;
		return this;
	}

	public LocalDateTime getSentAt() {
		return sentAt;
	}

	public void setSentAt(final LocalDateTime sentAt) {
		this.sentAt = sentAt;
	}

	public String getBody() {
		return body;
	}

	public void setBody(final String body) {
		this.body = body;
	}

	public MessageDetails withBody(final String body) {
		this.body = body;
		return this;
	}

	public MessageDetails withSentAt(final LocalDateTime sentAt) {
		this.sentAt = sentAt;
		return this;
	}

	public SigningStatus getSigningStatus() {
		return signingStatus;
	}

	public void setSigningStatus(final SigningStatus signingStatus) {
		this.signingStatus = signingStatus;
	}

	public MessageDetails withSigningStatus(final SigningStatus signingStatus) {
		this.signingStatus = signingStatus;
		return this;
	}

	public List<AttachmentDetails> getAttachments() {
		return attachments;
	}

	public void setAttachments(final List<AttachmentDetails> attachments) {
		this.attachments = attachments;
	}

	public MessageDetails withAttachments(final List<AttachmentDetails> attachments) {
		this.attachments = attachments;
		return this;
	}

	public List<RecipientDetails> getRecipients() {
		return recipients;
	}

	public void setRecipients(final List<RecipientDetails> recipients) {
		this.recipients = recipients;
	}

	public MessageDetails withRecipients(final List<RecipientDetails> recipients) {
		this.recipients = recipients;
		return this;
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
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final MessageDetails that = (MessageDetails) o;
		return Objects.equals(subject, that.subject) && Objects.equals(body, that.body) && Objects.equals(sentAt, that.sentAt) && Objects.equals(signingStatus, that.signingStatus) && Objects.equals(attachments, that.attachments) && Objects.equals(
			recipients, that.recipients);
	}

	@Override
	public int hashCode() {
		return Objects.hash(subject, body, sentAt, signingStatus, attachments, recipients);
	}

	public static class AttachmentDetails {

		@Schema(description = "Attachment ID", accessMode = Schema.AccessMode.READ_ONLY, examples = "123e4567-e89b-12d3-a456-426614174000")
		private String attachmentId;

		@Schema(description = "File name of the attachment", accessMode = Schema.AccessMode.READ_ONLY, examples = "document.pdf")
		private String fileName;

		@Schema(description = "MIME type of the attachment", accessMode = Schema.AccessMode.READ_ONLY, examples = "application/pdf")
		private String contentType;

		public static AttachmentDetails create() {
			return new AttachmentDetails();
		}

		public String getAttachmentId() {
			return attachmentId;
		}

		public void setAttachmentId(final String attachmentId) {
			this.attachmentId = attachmentId;
		}

		public AttachmentDetails withAttachmentId(final String attachmentId) {
			this.attachmentId = attachmentId;
			return this;
		}

		public String getFileName() {
			return fileName;
		}

		public void setFileName(final String fileName) {
			this.fileName = fileName;
		}

		public AttachmentDetails withFileName(final String fileName) {
			this.fileName = fileName;
			return this;
		}

		public String getContentType() {
			return contentType;
		}

		public void setContentType(final String contentType) {
			this.contentType = contentType;
		}

		public AttachmentDetails withContentType(final String mimeType) {
			this.contentType = mimeType;
			return this;
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
		public boolean equals(final Object o) {
			if (o == null || getClass() != o.getClass())
				return false;
			final AttachmentDetails that = (AttachmentDetails) o;
			return Objects.equals(attachmentId, that.attachmentId) && Objects.equals(fileName, that.fileName) && Objects.equals(contentType, that.contentType);
		}

		@Override
		public int hashCode() {
			return Objects.hash(attachmentId, fileName, contentType);
		}
	}

	public static class RecipientDetails {

		@Schema(description = "Name of the recipient", accessMode = Schema.AccessMode.READ_ONLY, examples = "John Doe")
		private String name;

		@Schema(description = "The recipients party ID", accessMode = Schema.AccessMode.READ_ONLY, examples = "1234567890")
		private String partyId;

		@Schema(description = "Mobile number", accessMode = Schema.AccessMode.READ_ONLY, examples = "+46701234567")
		private String mobileNumber;

		@Schema(description = "Street address", accessMode = Schema.AccessMode.READ_ONLY, examples = "Main Street 5")
		private String streetAddress;

		@Schema(description = "Zip code", accessMode = Schema.AccessMode.READ_ONLY, examples = "85751")
		private String zipCode;

		@Schema(description = "City", accessMode = Schema.AccessMode.READ_ONLY, examples = "Sundsvall")
		private String city;

		@Schema(description = "Message type", accessMode = Schema.AccessMode.READ_ONLY, examples = "SNAIL_MAIL|DIGITAL_MAIL|SMS")
		private String messageType;

		@Schema(description = "Status of the message to this recipient", accessMode = Schema.AccessMode.READ_ONLY, examples = "SENT|NOT_SENT|FAILED")
		private String status;

		public static RecipientDetails create() {
			return new RecipientDetails();
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(final String status) {
			this.status = status;
		}

		public RecipientDetails withStatus(final String status) {
			this.status = status;
			return this;
		}

		public String getMobileNumber() {
			return mobileNumber;
		}

		public void setMobileNumber(final String mobileNumber) {
			this.mobileNumber = mobileNumber;
		}

		public RecipientDetails withMobileNumber(final String mobileNumber) {
			this.mobileNumber = mobileNumber;
			return this;
		}

		public String getName() {
			return name;
		}

		public void setName(final String name) {
			this.name = name;
		}

		public RecipientDetails withName(final String name) {
			this.name = name;
			return this;
		}

		public String getPartyId() {
			return partyId;
		}

		public void setPartyId(final String partyId) {
			this.partyId = partyId;
		}

		public RecipientDetails withPartyId(final String partyId) {
			this.partyId = partyId;
			return this;
		}

		public String getStreetAddress() {
			return streetAddress;
		}

		public void setStreetAddress(final String streetAddress) {
			this.streetAddress = streetAddress;
		}

		public RecipientDetails withStreetAddress(final String streetAddress) {
			this.streetAddress = streetAddress;
			return this;
		}

		public String getZipCode() {
			return zipCode;
		}

		public void setZipCode(final String zipCode) {
			this.zipCode = zipCode;
		}

		public RecipientDetails withZipCode(final String zipCode) {
			this.zipCode = zipCode;
			return this;
		}

		public String getCity() {
			return city;
		}

		public void setCity(final String city) {
			this.city = city;
		}

		public RecipientDetails withCity(final String city) {
			this.city = city;
			return this;
		}

		public String getMessageType() {
			return messageType;
		}

		public void setMessageType(final String messageType) {
			this.messageType = messageType;
		}

		public RecipientDetails withMessageType(final String messageType) {
			this.messageType = messageType;
			return this;
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
		public boolean equals(final Object o) {
			if (o == null || getClass() != o.getClass())
				return false;
			final RecipientDetails that = (RecipientDetails) o;
			return Objects.equals(name, that.name) && Objects.equals(partyId, that.partyId) && Objects.equals(mobileNumber, that.mobileNumber) && Objects.equals(streetAddress, that.streetAddress) && Objects.equals(
				zipCode, that.zipCode) && Objects.equals(city, that.city) && Objects.equals(messageType, that.messageType) && Objects.equals(status, that.status);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, partyId, mobileNumber, streetAddress, zipCode, city, messageType, status);
		}
	}

}
