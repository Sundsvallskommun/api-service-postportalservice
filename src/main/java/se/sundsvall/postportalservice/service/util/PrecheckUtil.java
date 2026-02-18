package se.sundsvall.postportalservice.service.util;

import generated.se.sundsvall.messaging.Mailbox;
import java.util.List;
import java.util.Objects;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

public final class PrecheckUtil {

	private PrecheckUtil() {}

	/**
	 * Record for holding mailbox partyId with reason for unreachability.
	 *
	 * @param partyId partyId of the recipient
	 * @param reason  reason for unreachability (null if reachable)
	 */
	public record UnreachableMailbox(String partyId, String reason) {}

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
}
