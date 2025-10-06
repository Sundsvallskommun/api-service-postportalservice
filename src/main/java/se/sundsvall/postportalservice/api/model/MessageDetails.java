package se.sundsvall.postportalservice.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Schema(description = "Message details model")
public class MessageDetails {

	@Schema(description = "Message subject", accessMode = Schema.AccessMode.READ_ONLY, example = "This is a subject")
	private String subject;

	@Schema(description = "When the message was sent", accessMode = Schema.AccessMode.READ_ONLY, example = "2021-01-01T12:00:00")
	private LocalDateTime sentAt;

	@ArraySchema(schema = @Schema(description = "List of attachments", implementation = Attachment.class, accessMode = Schema.AccessMode.READ_ONLY))
	private List<Attachment> attachments;

	@ArraySchema(schema = @Schema(description = "List of recipients", implementation = Recipient.class, accessMode = Schema.AccessMode.READ_ONLY))
	private List<Recipient> recipients;

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

	public MessageDetails withSentAt(LocalDateTime sentAt) {
		this.sentAt = sentAt;
		return this;
	}

	public void setSentAt(LocalDateTime sentAt) {
		this.sentAt = sentAt;
	}

	public List<Attachment> getAttachments() {
		return attachments;
	}

	public MessageDetails withAttachments(List<Attachment> attachments) {
		this.attachments = attachments;
		return this;
	}

	public void setAttachments(List<Attachment> attachments) {
		this.attachments = attachments;
	}

	public List<Recipient> getRecipients() {
		return recipients;
	}

	public MessageDetails withRecipients(List<Recipient> recipients) {
		this.recipients = recipients;
		return this;
	}

	public void setRecipients(List<Recipient> recipients) {
		this.recipients = recipients;
	}

	@Override
	public String toString() {
		return "MessageDetails{" +
			"subject='" + subject + '\'' +
			", sentAt=" + sentAt +
			", attachments=" + attachments +
			", recipients=" + recipients +
			'}';
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		MessageDetails that = (MessageDetails) o;
		return Objects.equals(subject, that.subject) && Objects.equals(sentAt, that.sentAt) && Objects.equals(attachments, that.attachments) && Objects.equals(recipients, that.recipients);
	}

	@Override
	public int hashCode() {
		return Objects.hash(subject, sentAt, attachments, recipients);
	}

	public static class Attachment {

		@Schema(description = "Attachment ID", accessMode = Schema.AccessMode.READ_ONLY, example = "123e4567-e89b-12d3-a456-426614174000")
		private String attachmentId;

		@Schema(description = "File name of the attachment", accessMode = Schema.AccessMode.READ_ONLY, example = "document.pdf")
		private String fileName;

		@Schema(description = "MIME type of the attachment", accessMode = Schema.AccessMode.READ_ONLY, example = "application/pdf")
		private String contentType;

		public static Attachment create() {
			return new Attachment();
		}

		public String getAttachmentId() {
			return attachmentId;
		}

		public Attachment withAttachmentId(String attachmentId) {
			this.attachmentId = attachmentId;
			return this;
		}

		public void setAttachmentId(String attachmentId) {
			this.attachmentId = attachmentId;
		}

		public String getFileName() {
			return fileName;
		}

		public Attachment withFileName(String fileName) {
			this.fileName = fileName;
			return this;
		}

		public void setFileName(String fileName) {
			this.fileName = fileName;
		}

		public String getContentType() {
			return contentType;
		}

		public Attachment withContentType(String mimeType) {
			this.contentType = mimeType;
			return this;
		}

		public void setContentType(String contentType) {
			this.contentType = contentType;
		}

		@Override
		public String toString() {
			return "Attachment{" +
				"attachmentId='" + attachmentId + '\'' +
				", fileName='" + fileName + '\'' +
				", contentType='" + contentType + '\'' +
				'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass())
				return false;
			Attachment that = (Attachment) o;
			return Objects.equals(attachmentId, that.attachmentId) && Objects.equals(fileName, that.fileName) && Objects.equals(contentType, that.contentType);
		}

		@Override
		public int hashCode() {
			return Objects.hash(attachmentId, fileName, contentType);
		}
	}

	public static class Recipient {

		@Schema(description = "Name of the recipient", accessMode = Schema.AccessMode.READ_ONLY, example = "John Doe")
		private String name;

		@Schema(description = "The recipients party ID", accessMode = Schema.AccessMode.READ_ONLY, example = "1234567890")
		private String partyId;

		@Schema(description = "Mobile number", accessMode = Schema.AccessMode.READ_ONLY, example = "+46701740605")
		private String mobileNumber;

		@Schema(description = "Street address", accessMode = Schema.AccessMode.READ_ONLY, example = "Main Street 5")
		private String streetAddress;

		@Schema(description = "Zip code", accessMode = Schema.AccessMode.READ_ONLY, example = "85751")
		private String zipCode;

		@Schema(description = "City", accessMode = Schema.AccessMode.READ_ONLY, example = "Sundsvall")
		private String city;

		@Schema(description = "Message type", accessMode = Schema.AccessMode.READ_ONLY, example = "SNAIL_MAIL|DIGITAL_MAIL|SMS")
		private String messageType;

		public static Recipient create() {
			return new Recipient();
		}

		public String getMobileNumber() {
			return mobileNumber;
		}

		public Recipient withMobileNumber(String mobileNumber) {
			this.mobileNumber = mobileNumber;
			return this;
		}

		public void setMobileNumber(String mobileNumber) {
			this.mobileNumber = mobileNumber;
		}

		public String getName() {
			return name;
		}

		public Recipient withName(String name) {
			this.name = name;
			return this;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getPartyId() {
			return partyId;
		}

		public Recipient withPartyId(String partyId) {
			this.partyId = partyId;
			return this;
		}

		public void setPartyId(String partyId) {
			this.partyId = partyId;
		}

		public String getStreetAddress() {
			return streetAddress;
		}

		public Recipient withStreetAddress(String streetAddress) {
			this.streetAddress = streetAddress;
			return this;
		}

		public void setStreetAddress(String streetAddress) {
			this.streetAddress = streetAddress;
		}

		public String getZipCode() {
			return zipCode;
		}

		public Recipient withZipCode(String zipCode) {
			this.zipCode = zipCode;
			return this;
		}

		public void setZipCode(String zipCode) {
			this.zipCode = zipCode;
		}

		public String getCity() {
			return city;
		}

		public Recipient withCity(String city) {
			this.city = city;
			return this;
		}

		public void setCity(String city) {
			this.city = city;
		}

		public String getMessageType() {
			return messageType;
		}

		public Recipient withMessageType(String messageType) {
			this.messageType = messageType;
			return this;
		}

		public void setMessageType(String messageType) {
			this.messageType = messageType;
		}

		@Override
		public String toString() {
			return "Recipient{" +
				"name='" + name + '\'' +
				", partyId='" + partyId + '\'' +
				", mobileNumber='" + mobileNumber + '\'' +
				", streetAddress='" + streetAddress + '\'' +
				", zipCode='" + zipCode + '\'' +
				", city='" + city + '\'' +
				", messageType='" + messageType + '\'' +
				'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass())
				return false;
			Recipient recipient = (Recipient) o;
			return Objects.equals(name, recipient.name) && Objects.equals(partyId, recipient.partyId) && Objects.equals(mobileNumber, recipient.mobileNumber) && Objects.equals(streetAddress, recipient.streetAddress)
				&& Objects.equals(zipCode, recipient.zipCode) && Objects.equals(city, recipient.city) && Objects.equals(messageType, recipient.messageType);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, partyId, mobileNumber, streetAddress, zipCode, city, messageType);
		}
	}

}
