package se.sundsvall.postportalservice.service.util;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
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

	/**
	 * Record for holding mailbox partyId with reason for unreachability.
	 *
	 * @param partyId partyId of the recipient
	 * @param reason  reason for unreachability (null if reachable)
	 */
	public record UnreachableMailbox(String partyId, String reason) {}

	public static List<PersonGuidBatch> filterSuccessfulPersonGuidBatches(final List<PersonGuidBatch> batches) {
		return ofNullable(batches).orElse(emptyList()).stream()
			.filter(batch -> TRUE.equals(batch.getSuccess()))
			.toList();
	}

	public static List<String> filterNonNull(final Map<String, String> pinToParty) {
		return ofNullable(pinToParty).orElse(Map.of())
			.values()
			.stream()
			.filter(Objects::nonNull)
			.toList();
	}

	public static List<String> filterReachableMailboxes(final List<Mailbox> mailboxes) {
		return ofNullable(mailboxes).orElse(emptyList()).stream()
			.filter(mailbox -> TRUE.equals(mailbox.getReachable()))
			.map(Mailbox::getPartyId)
			.filter(Objects::nonNull)
			.toList();
	}

	public static List<UnreachableMailbox> filterUnreachableMailboxesWithReason(final List<Mailbox> mailboxes) {
		return ofNullable(mailboxes).orElse(emptyList()).stream()
			.filter(mailbox -> FALSE.equals(mailbox.getReachable()))
			.filter(mailbox -> mailbox.getPartyId() != null)
			.map(mailbox -> new UnreachableMailbox(mailbox.getPartyId(), mailbox.getReason()))
			.toList();
	}

	public static List<String> filterUnreachableMailboxes(final List<Mailbox> mailboxes) {
		return filterUnreachableMailboxesWithReason(mailboxes).stream()
			.map(UnreachableMailbox::partyId)
			.toList();
	}

	public static DeliveryMethod getDeliveryMethod(final String partyId, final List<String> hasDigitalMailbox, final List<String> canReceiveSnailMail) {
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
