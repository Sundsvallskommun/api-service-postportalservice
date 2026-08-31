package se.sundsvall.postportalservice.integration.rabbitmq;

import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;

import static org.apache.commons.lang3.ObjectUtils.anyNull;
import static se.sundsvall.postportalservice.Constants.ORIGIN;
import static se.sundsvall.postportalservice.service.util.IdentifierUtil.getIdentifierHeaderValue;

public final class RabbitMapper {

	private RabbitMapper() {}

	public static SmsQueueMessage toSmsQueueMessage(final MessageEntity messageEntity, final RecipientEntity recipientEntity) {
		if (anyNull(messageEntity, recipientEntity)) {
			return null;
		}
		return new SmsQueueMessage(
			messageEntity.getMunicipalityId(),
			messageEntity.getId(),
			recipientEntity.getId(),
			recipientEntity.getPartyId(),
			recipientEntity.getPhoneNumber(),
			messageEntity.getDisplayName(),
			messageEntity.getDepartment().getName(),
			messageEntity.getBody(),
			getIdentifierHeaderValue(messageEntity.getUser().getUsername()),
			ORIGIN);
	}
}
