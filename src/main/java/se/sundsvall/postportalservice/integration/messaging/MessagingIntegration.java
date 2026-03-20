package se.sundsvall.postportalservice.integration.messaging;

import generated.se.sundsvall.messaging.EmailAttachment;
import generated.se.sundsvall.messaging.EmailRequest;
import generated.se.sundsvall.messaging.EmailRequestParty;
import generated.se.sundsvall.messaging.Mailbox;
import generated.se.sundsvall.messaging.MessageBatchResult;
import generated.se.sundsvall.messaging.MessageResult;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.service.util.RecipientId;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static se.sundsvall.postportalservice.Constants.ORIGIN;
import static se.sundsvall.postportalservice.integration.messaging.MessagingMapper.toDigitalMailRequest;
import static se.sundsvall.postportalservice.integration.messaging.MessagingMapper.toSmsRequest;
import static se.sundsvall.postportalservice.integration.messaging.MessagingMapper.toSnailmailRequest;
import static se.sundsvall.postportalservice.service.util.CallbackEmailUtil.getCallbackEmail;
import static se.sundsvall.postportalservice.service.util.CallbackEmailUtil.getCallbackEmailSubject;
import static se.sundsvall.postportalservice.service.util.CallbackEmailUtil.getEmailBody;
import static se.sundsvall.postportalservice.service.util.IdentifierUtil.getIdentifierHeaderValue;

@Component
public class MessagingIntegration {

	private final MessagingClient client;

	public MessagingIntegration(final MessagingClient client) {
		this.client = client;
	}

	public MessageBatchResult sendDigitalMail(final MessageEntity messageEntity, final RecipientEntity recipientEntity) {
		RecipientId.init(recipientEntity.getId());
		final var digitalMailRequest = toDigitalMailRequest(messageEntity, recipientEntity.getPartyId());

		return client.sendDigitalMail(getIdentifierHeaderValue(messageEntity.getUser().getUsername()),
			ORIGIN,
			messageEntity.getMunicipalityId(),
			messageEntity.getDepartment().getOrganizationNumber(),
			digitalMailRequest);
	}

	public MessageResult sendSnailMail(final MessageEntity messageEntity, final RecipientEntity recipientEntity) {
		RecipientId.init(recipientEntity.getId());
		final var snailmailRequest = toSnailmailRequest(messageEntity, recipientEntity);

		return client.sendSnailMail(getIdentifierHeaderValue(messageEntity.getUser().getUsername()),
			ORIGIN,
			messageEntity.getMunicipalityId(),
			snailmailRequest,
			messageEntity.getId());
	}

	public void triggerSnailMailBatchProcessing(final String municipalityId, final String batchId) {
		client.triggerSnailMailBatchProcessing(municipalityId, batchId);
	}

	public MessageResult sendSms(final MessageEntity messageEntity, final RecipientEntity recipientEntity) {
		RecipientId.init(recipientEntity.getId());
		final var smsRequest = toSmsRequest(messageEntity, recipientEntity);

		return client.sendSms(getIdentifierHeaderValue(messageEntity.getUser().getUsername()),
			ORIGIN,
			messageEntity.getMunicipalityId(),
			smsRequest);
	}

	public List<Mailbox> precheckMailboxes(final String municipalityId, final String organizationNumber, final List<String> partyIds) {
		return client.precheckMailboxes(municipalityId, organizationNumber, partyIds);
	}

	public MessageResult sendCallbackEmail(final MessageEntity messageEntity, final RecipientEntity recipientEntity, final Map<String, String> settingsMap) {
		RecipientId.init(recipientEntity.getId());

		// Extract email, subject and body from messagingSettings
		final var emailAddress = getCallbackEmail(settingsMap);
		final var emailSubject = getCallbackEmailSubject(settingsMap);
		final var emailBody = getEmailBody(settingsMap);

		// sanity-check that we have everything we need
		checkRequiredParameters(emailAddress, emailSubject, emailBody);

		// Create attachments to be included in the email
		final var emailAttachments = new ArrayList<EmailAttachment>();
		ofNullable(messageEntity.getAttachments()).orElse(emptyList())
			.forEach(entity -> emailAttachments.add(
				new EmailAttachment()
					.contentType(entity.getContentType())
					.name(entity.getFileName())
					.content(blobToBase64(entity.getContent()))));

		// Create the request
		final var emailRequest = new EmailRequest()
			.party(new EmailRequestParty().partyId(UUID.fromString(recipientEntity.getPartyId())))
			.emailAddress(emailAddress)
			.subject(emailSubject)
			.htmlMessage(emailBody)
			.attachments(emailAttachments);

		return client.sendEmail(
			getIdentifierHeaderValue(messageEntity.getUser().getUsername()),
			ORIGIN,
			messageEntity.getMunicipalityId(),
			emailRequest,
			true);
	}

	private void checkRequiredParameters(final String emailAddress, final String emailSubject, final String emailBody) {
		// Verify that we got the required parameters to send the email, if not, throw an exception
		if (isBlank(emailAddress) || isBlank(emailSubject) || isBlank(emailBody)) {
			throw Problem.valueOf(BAD_GATEWAY, "Missing required parameter for callback email. " +
				"emailAddress: " + emailAddress + ", " +
				"emailSubject: " + emailSubject + ", " +
				"emailBody: " + (isBlank(emailBody) ? "blank" : "present"));
		}
	}

	private String blobToBase64(final Blob blob) {
		if (blob != null) {
			try {
				byte[] bytes = blob.getBytes(1, (int) blob.length());
				return Base64.getEncoder().encodeToString(bytes);
			} catch (SQLException e) {
				throw Problem.valueOf(BAD_GATEWAY, "Couldn't read blob from entity, not sending email. " + e.getMessage());
			}
		}
		throw Problem.valueOf(BAD_GATEWAY, "No attachment, nothing to attach in email.");
	}
}
