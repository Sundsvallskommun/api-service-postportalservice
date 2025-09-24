package se.sundsvall.postportalservice.integration.messaging;

import static se.sundsvall.postportalservice.Constants.ORIGIN;
import static se.sundsvall.postportalservice.integration.messaging.MessagingMapper.toDigitalMailRequest;
import static se.sundsvall.postportalservice.integration.messaging.MessagingMapper.toSmsRequest;
import static se.sundsvall.postportalservice.integration.messaging.MessagingMapper.toSnailmailRequest;

import generated.se.sundsvall.messaging.Mailbox;
import generated.se.sundsvall.messaging.MessageBatchResult;
import generated.se.sundsvall.messaging.MessageResult;
import java.util.List;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;

@Component
public class MessagingIntegration {

	private final MessagingClient client;

	public MessagingIntegration(final MessagingClient client) {
		this.client = client;
	}

	public MessageBatchResult sendDigitalMail(final MessageEntity messageEntity, final RecipientEntity recipientEntity) {
		var digitalMailRequest = toDigitalMailRequest(messageEntity, recipientEntity.getPartyId());

		return client.sendDigitalMail(getIdentifierHeaderValue(messageEntity.getUser().getName()),
			ORIGIN,
			messageEntity.getMunicipalityId(),
			digitalMailRequest);
	}

	public MessageResult sendSnailMail(final MessageEntity messageEntity, final RecipientEntity recipientEntity) {
		var snailmailRequest = toSnailmailRequest(messageEntity, recipientEntity);

		return client.sendSnailMail(getIdentifierHeaderValue(messageEntity.getUser().getName()),
			ORIGIN,
			messageEntity.getMunicipalityId(),
			snailmailRequest,
			messageEntity.getId());
	}

	public void triggerSnailMailBatchProcessing(final String municipalityId, final String batchId) {
		client.triggerSnailMailBatchProcessing(municipalityId, batchId);
	}

	public MessageResult sendSms(final MessageEntity messageEntity, final RecipientEntity recipientEntity) {
		var smsRequest = toSmsRequest(messageEntity, recipientEntity);
		smsRequest.setSender(messageEntity.getDisplayName());

		return client.sendSms(getIdentifierHeaderValue(messageEntity.getUser().getName()),
			ORIGIN,
			messageEntity.getMunicipalityId(),
			smsRequest);
	}

	public boolean sendSmsBatch(final String municipalityId) {
		// Used to send SMS in batch.
		// TODO: Implement the mapping and the call to messaging. Also, the response from messaging should be used to update the
		// status of the message.
		return true;
	}

	public List<Mailbox> precheckMailboxes(final String municipalityId, final String organizationNumber, final List<String> partyIds) {
		return client.precheckMailboxes(municipalityId, organizationNumber, partyIds);
	}

	String getIdentifierHeaderValue(final String userName) {
		return Identifier.create()
			.withType(Identifier.Type.AD_ACCOUNT)
			.withValue(userName)
			.toHeaderValue();
	}

}
