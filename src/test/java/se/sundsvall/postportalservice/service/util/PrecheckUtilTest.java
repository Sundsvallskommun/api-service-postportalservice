package se.sundsvall.postportalservice.service.util;

import generated.se.sundsvall.messaging.Mailbox;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PrecheckUtilTest {

	@Test
	void filterReachableMailboxes() {
		final var mailbox1 = createMailbox("11111111-1111-1111-1111-111111111111", true);
		final var mailbox2 = createMailbox("22222222-2222-2222-2222-222222222222", false);
		final var mailbox3 = createMailbox(null, true);

		final var reachable = PrecheckUtil.filterReachableMailboxes(List.of(mailbox1, mailbox2, mailbox3));

		assertThat(reachable).containsExactly("11111111-1111-1111-1111-111111111111");
	}

	@Test
	void filterUnreachableMailboxesWithReason() {
		final var mailbox1 = createMailbox("11111111-1111-1111-1111-111111111111", true);
		final var mailbox2 = createMailboxWithReason("22222222-2222-2222-2222-222222222222", false, "No digital mailbox");
		final var mailbox3 = createMailbox(null, false);

		final var unreachable = PrecheckUtil.filterUnreachableMailboxesWithReason(List.of(mailbox1, mailbox2, mailbox3));

		assertThat(unreachable).hasSize(1);
		assertThat(unreachable.getFirst().partyId()).isEqualTo("22222222-2222-2222-2222-222222222222");
		assertThat(unreachable.getFirst().reason()).isEqualTo("No digital mailbox");
	}

	@Test
	void filterReachableMailboxes_nullInput() {
		assertThat(PrecheckUtil.filterReachableMailboxes(null)).isEmpty();
	}

	@Test
	void filterUnreachableMailboxesWithReason_nullInput() {
		assertThat(PrecheckUtil.filterUnreachableMailboxesWithReason(null)).isEmpty();
	}

	private Mailbox createMailbox(String partyId, Boolean reachable) {
		final var mailbox = new Mailbox();
		mailbox.setPartyId(partyId);
		mailbox.setReachable(reachable);
		return mailbox;
	}

	private Mailbox createMailboxWithReason(String partyId, Boolean reachable, String reason) {
		final var mailbox = createMailbox(partyId, reachable);
		mailbox.setReason(reason);
		return mailbox;
	}
}
