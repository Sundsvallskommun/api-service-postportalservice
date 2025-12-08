package se.sundsvall.postportalservice.api.model;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Objects;

@Schema(description = "Message model")
public class Message {

	@Schema(description = "Message ID", accessMode = READ_ONLY, examples = "123456")
	private String messageId;

	@Schema(description = "The subject", accessMode = READ_ONLY, examples = "Important information")
	private String subject;

	@Schema(description = "Type of message", accessMode = READ_ONLY, examples = {
		"SMS", "LETTER", "DIGITAL_REGISTERED_LETTER"
	})
	private String type;

	@Schema(description = "When the message was sent", accessMode = READ_ONLY, examples = "2021-01-01T12:00:00")
	private LocalDateTime sentAt;

	@Schema(description = "Status for signing process. Only applicable for message type DIGITAL_REGISTERED_LETTER", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY)
	private SigningStatus signingStatus;

	@Schema(description = "Total number of recipients to whom the message has been sent", accessMode = READ_ONLY, examples = "12")
	private int numberOfRecipients;

	public static Message create() {
		return new Message();
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(final String messageId) {
		this.messageId = messageId;
	}

	public Message withMessageId(final String messageId) {
		this.messageId = messageId;
		return this;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(final String subject) {
		this.subject = subject;
	}

	public Message withSubject(final String subject) {
		this.subject = subject;
		return this;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public Message withType(final String type) {
		this.type = type;
		return this;
	}

	public LocalDateTime getSentAt() {
		return sentAt;
	}

	public void setSentAt(final LocalDateTime sentAt) {
		this.sentAt = sentAt;
	}

	public Message withSentAt(final LocalDateTime sentAt) {
		this.sentAt = sentAt;
		return this;
	}

	public SigningStatus getSigningStatus() {
		return signingStatus;
	}

	public void setSigningStatus(final SigningStatus signingStatus) {
		this.signingStatus = signingStatus;
	}

	public Message withSigningStatus(final SigningStatus signingStatus) {
		this.signingStatus = signingStatus;
		return this;
	}

	public int getNumberOfRecipients() {
		return numberOfRecipients;
	}

	public void setNumberOfRecipients(final int numberOfRecipients) {
		this.numberOfRecipients = numberOfRecipients;
	}

	public Message withNumberOfRecipients(final int numberOfRecipients) {
		this.numberOfRecipients = numberOfRecipients;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(messageId, numberOfRecipients, sentAt, signingStatus, subject, type);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final Message other)) {
			return false;
		}
		return Objects.equals(messageId, other.messageId) && numberOfRecipients == other.numberOfRecipients && Objects.equals(sentAt, other.sentAt) && Objects.equals(signingStatus, other.signingStatus) && Objects.equals(subject, other.subject) && Objects
			.equals(type, other.type);
	}

	@Override
	public String toString() {
		return "Message [messageId=" + messageId + ", subject=" + subject + ", type=" + type + ", sentAt=" + sentAt + ", signingStatus=" + signingStatus + ", numberOfRecipients="
			+ numberOfRecipients + "]";
	}

}
