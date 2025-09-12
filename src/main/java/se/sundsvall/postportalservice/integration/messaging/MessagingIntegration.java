package se.sundsvall.postportalservice.integration.messaging;

import static se.sundsvall.postportalservice.integration.messaging.MessagingMapper.toSmsRequest;

import generated.se.sundsvall.messaging.MessageResult;
import org.springframework.stereotype.Component;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration;

@Component
public class MessagingIntegration {

	private final MessagingClient client;
	private final MessagingSettingsIntegration messagingSettingsIntegration;

	public MessagingIntegration(
		final MessagingClient client,
		final MessagingSettingsIntegration messagingSettingsIntegration) {
		this.client = client;
		this.messagingSettingsIntegration = messagingSettingsIntegration;
	}

	public boolean sendDigitalMail(final String municipalityId) {
		// Used to send digital mail.
		// TODO: Implement the mapping and the call to messaging. Also, the response from messaging should be used to update the
		// status of the message.
		return true;
	}

	public MessageResult sendSms(final MessageEntity messageEntity, final RecipientEntity recipientEntity) {
		var municipalityId = messageEntity.getMunicipalityId();

		var smsRequest = toSmsRequest(messageEntity, recipientEntity);
		smsRequest.setSender(messageEntity.getDisplayName());

		return client.sendSms("type=adAccount; " + messageEntity.getUser().getName(), messageEntity.getDepartment().getName(), municipalityId, smsRequest);
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

	public boolean precheckMailBoxes(final String municipalityId) {
		// Used to precheck digital-mailboxes.
		// TODO: When messaging exposes the new precheck endpoint, implement the mapping and the call to messaging. The return
		// value should include which recipients that have an eligible digital-mailbox.
		return true;
	}

}
