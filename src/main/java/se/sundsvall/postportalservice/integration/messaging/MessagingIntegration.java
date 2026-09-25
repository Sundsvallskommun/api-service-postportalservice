package se.sundsvall.postportalservice.integration.messaging;

import generated.se.sundsvall.messaging.EmailSender;
import generated.se.sundsvall.messaging.Mailbox;
import generated.se.sundsvall.messaging.MessageBatchResult;
import generated.se.sundsvall.messaging.MessageResult;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.messaging.configuration.MessagingProperties;
import se.sundsvall.postportalservice.service.util.RecipientId;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static se.sundsvall.postportalservice.Constants.ORIGIN;
import static se.sundsvall.postportalservice.integration.messaging.MessagingMapper.toDigitalMailRequest;
import static se.sundsvall.postportalservice.integration.messaging.MessagingMapper.toEmailAttachments;
import static se.sundsvall.postportalservice.integration.messaging.MessagingMapper.toEmailRequest;
import static se.sundsvall.postportalservice.integration.messaging.MessagingMapper.toEmailSender;
import static se.sundsvall.postportalservice.integration.messaging.MessagingMapper.toSmsRequest;
import static se.sundsvall.postportalservice.integration.messaging.MessagingMapper.toSnailmailRequest;
import static se.sundsvall.postportalservice.service.util.IdentifierUtil.getIdentifierHeaderValue;

@Component
public class MessagingIntegration {

	private static final Logger LOG = LoggerFactory.getLogger(MessagingIntegration.class);

	private final MessagingClient client;
	private final MessagingProperties properties;

	public MessagingIntegration(final MessagingClient client, final MessagingProperties properties) {
		this.client = client;
		this.properties = properties;
	}

	public MessageBatchResult sendDigitalMail(final MessageEntity messageEntity, final RecipientEntity recipientEntity) {
		LOG.info("Sending digital mail to recipient with id {}", recipientEntity.getId());
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

	public MessageResult sendSms(final MessageEntity messageEntity, final RecipientEntity recipientEntity) {
		LOG.info("Sending SMS to recipient with id {}", recipientEntity.getId());
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
		final var emailRequest = toEmailRequest(recipientEntity, settingsMap, resolveCallbackEmailSender(messageEntity.getMunicipalityId()));
		final var emailAttachments = toEmailAttachments(messageEntity);
		emailRequest.setAttachments(emailAttachments);

		return client.sendEmail(
			getIdentifierHeaderValue(messageEntity.getUser().getUsername()),
			ORIGIN,
			messageEntity.getMunicipalityId(),
			emailRequest,
			false);
	}

	/**
	 * Resolves the sender of the callback e-mail for the given municipality. The sender address is municipality specific
	 * (it has to match a domain the municipality is allowed to send from) and is configured under
	 * integration.messaging.callback-email-sender.addresses.
	 *
	 * @param  municipalityId   the municipality to resolve the sender for
	 * @return                  the sender to use on the callback e-mail
	 * @throws ThrowableProblem if no sender address is configured for the municipality
	 */
	public EmailSender resolveCallbackEmailSender(final String municipalityId) {
		final var callbackEmailSender = properties.callbackEmailSender();
		final var address = callbackEmailSender.addresses().get(municipalityId);
		if (isBlank(address)) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, "No callback e-mail sender address configured for municipalityId '%s'".formatted(municipalityId));
		}
		return toEmailSender(callbackEmailSender.name(), address);
	}
}
