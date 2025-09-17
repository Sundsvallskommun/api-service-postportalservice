package se.sundsvall.postportalservice.integration.messaging;

import static se.sundsvall.postportalservice.integration.messaging.MessagingMapper.toDigitalMailRequest;
import static se.sundsvall.postportalservice.integration.messaging.MessagingMapper.toSmsRequest;

import generated.se.sundsvall.messaging.DeliveryResult;
import generated.se.sundsvall.messaging.Mailbox;
import generated.se.sundsvall.messaging.MessageBatchResult;
import generated.se.sundsvall.messaging.MessageResult;
import generated.se.sundsvall.messaging.MessageStatus;
import java.util.List;
import java.util.UUID;
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
		var digitalMailRequest = toDigitalMailRequest(messageEntity, recipientEntity);

		return client.sendDigitalMail(getIdentifierHeaderValue(messageEntity.getUser().getName()),
			messageEntity.getDepartment().getName(),
			messageEntity.getMunicipalityId(),
			digitalMailRequest);
	}

	public MessageResult sendSnailMail(final MessageEntity messageEntity, final RecipientEntity recipientEntity) {
		// TODO: Implement mapping to snailmail request, call messaging and return the result.

		// Temporary hardcoded response until implementation is done, this is done for integration testing purposes.
		client.sendSnailMail(getIdentifierHeaderValue(messageEntity.getUser().getName()),
			messageEntity.getDepartment().getName(),
			messageEntity.getMunicipalityId());
		return new MessageResult()
			.messageId(UUID.fromString("550e8400-e29b-41d4-a716-446655440002"))
			.deliveries(List.of(new DeliveryResult().status(MessageStatus.SENT)));
	}

	public MessageResult sendSms(final MessageEntity messageEntity, final RecipientEntity recipientEntity) {
		var smsRequest = toSmsRequest(messageEntity, recipientEntity);
		smsRequest.setSender(messageEntity.getDisplayName());

		return client.sendSms(getIdentifierHeaderValue(messageEntity.getUser().getName()),
			messageEntity.getDepartment().getName(),
			messageEntity.getMunicipalityId(),
			smsRequest);
	}

	public boolean sendSmsBatch(final String municipalityId) {
		// Used to send SMS in batch.
		// TODO: Implement the mapping and the call to messaging. Also, the response from messaging should be used to update the
		// status of the message.
		return true;
	}

	public boolean sendSnailMail(final String municipalityId) {
		// Used to send snail mail.
		// TODO: When messaging exposes the new snail mail endpoint, implement the mapping and the call to messaging. Also, the
		// response from messaging should be used to update the status of the message.
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
