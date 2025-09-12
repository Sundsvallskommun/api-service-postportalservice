package se.sundsvall.postportalservice.integration.messaging;

import generated.se.sundsvall.messaging.DigitalMailRequest;
import generated.se.sundsvall.messaging.SmsBatchRequest;
import generated.se.sundsvall.messaging.SmsRequest;
import generated.se.sundsvall.messaging.SmsRequestParty;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;

public final class MessagingMapper {

	private MessagingMapper() {}

	public static SmsRequest toSmsRequest(final MessageEntity messageEntity, final RecipientEntity recipient) {
		return new SmsRequest()
			.message(messageEntity.getText())
			.department(messageEntity.getDepartment().getName())
			.mobileNumber(recipient.getPhoneNumber())
			.party(new SmsRequestParty().partyId(recipient.getPartyId()));
	}

	public static SmsBatchRequest toSmsBatchRequest() {
		return new SmsBatchRequest();
	}

	public static DigitalMailRequest toDigitalMailRequest() {
		return new DigitalMailRequest();
	}

}
