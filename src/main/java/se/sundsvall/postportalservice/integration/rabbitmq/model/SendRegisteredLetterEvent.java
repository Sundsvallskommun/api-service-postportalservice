package se.sundsvall.postportalservice.integration.rabbitmq.model;

import java.util.List;

public record SendRegisteredLetterEvent(
	String municipalityId,
	String requestId,
	String recipientId,
	Sender sender,
	Recipient recipient,
	Message message) {

	public record Sender(
		String identifier,
		String organizationNumber,
		String organizationName,
		String supportText,
		String contactInformationUrl,
		String contactInformationEmail,
		String contactInformationPhoneNumber) {}

	public record Recipient(
		String partyId) {}

	public record Message(
		String subject,
		String body,
		String contentType,
		List<String> attachmentIds) {}
}
