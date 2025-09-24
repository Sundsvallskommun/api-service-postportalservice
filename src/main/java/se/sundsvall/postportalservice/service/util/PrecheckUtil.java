package se.sundsvall.postportalservice.service.util;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static se.sundsvall.postportalservice.api.model.PrecheckResponse.DeliveryMethod.DELIVERY_NOT_POSSIBLE;
import static se.sundsvall.postportalservice.api.model.PrecheckResponse.DeliveryMethod.DIGITAL_MAIL;
import static se.sundsvall.postportalservice.api.model.PrecheckResponse.DeliveryMethod.SNAIL_MAIL;

import generated.se.sundsvall.citizen.PersonGuidBatch;
import generated.se.sundsvall.messaging.Mailbox;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import se.sundsvall.postportalservice.api.model.PrecheckResponse.DeliveryMethod;

public final class PrecheckUtil {

	private PrecheckUtil() {}

	public static List<PersonGuidBatch> filterSuccessfulPersonGuidBatches(List<PersonGuidBatch> batches) {
		return batches.stream()
			.filter(batch -> TRUE.equals(batch.getSuccess()))
			.toList();
	}

	public static List<String> filterNonNull(Map<String, String> pinToParty) {
		return pinToParty.values()
			.stream()
			.filter(Objects::nonNull)
			.toList();
	}

	public static List<String> filterReachableMailboxes(List<Mailbox> mailboxes) {
		return mailboxes.stream()
			.filter(mailbox -> TRUE.equals(mailbox.getReachable()))
			.map(Mailbox::getPartyId)
			.filter(Objects::nonNull)
			.toList();
	}

	public static List<String> filterUnreachableMailboxes(List<Mailbox> mailboxes) {
		return mailboxes.stream()
			.filter(mailbox -> FALSE.equals(mailbox.getReachable()))
			.map(Mailbox::getPartyId)
			.filter(Objects::nonNull)
			.toList();
	}

	public static DeliveryMethod getDeliveryMethod(String partyId, List<String> hasDigitalMailbox, List<String> canReceiveSnailMail) {
		if (partyId == null) {
			return DELIVERY_NOT_POSSIBLE;
		}

		if (hasDigitalMailbox.contains(partyId)) {
			return DIGITAL_MAIL;
		}

		if (canReceiveSnailMail.contains(partyId)) {
			return SNAIL_MAIL;
		}

		return DELIVERY_NOT_POSSIBLE;
	}
}
