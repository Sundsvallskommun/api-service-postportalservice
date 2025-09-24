package se.sundsvall.postportalservice.service.util;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.postportalservice.api.model.PrecheckResponse.DeliveryMethod.*;

import generated.se.sundsvall.citizen.PersonGuidBatch;
import generated.se.sundsvall.messaging.Mailbox;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PrecheckUtilTest {

	@Test
	void filterSuccessfulPersonGuidBatches() {
		final var ok = createPersonGuidBatch(true);
		final var fail = createPersonGuidBatch(false);

		assertThat(PrecheckUtil.filterSuccessfulPersonGuidBatches(List.of(ok, fail))).containsExactly(ok);
	}

	@Test
	void filterNonNull() {
		final var map = new LinkedHashMap<String, String>();

		map.put("191111111111", "11111111-1111-1111-1111-111111111111");
		map.put("192222222222", null);
		map.put("193333333333", "22222222-2222-2222-2222-222222222222");

		assertThat(PrecheckUtil.filterNonNull(map))
			.containsExactlyInAnyOrder("11111111-1111-1111-1111-111111111111", "22222222-2222-2222-2222-222222222222");
	}

	@Test
	void filterReachableMailboxes_and_UnreachableMailboxes() {
		final var mailbox1 = createMailbox("11111111-1111-1111-1111-111111111111", true);
		final var mailbox2 = createMailbox("22222222-2222-2222-2222-222222222222", false);
		final var mailbox3 = createMailbox(null, true);

		final var reachable = PrecheckUtil.filterReachableMailboxes(List.of(mailbox1, mailbox2, mailbox3));
		final var unreachable = PrecheckUtil.filterUnreachableMailboxes(List.of(mailbox1, mailbox2, mailbox3));

		assertThat(reachable).containsExactly("11111111-1111-1111-1111-111111111111");
		assertThat(unreachable).containsExactly("22222222-2222-2222-2222-222222222222");
	}

	@Test
	void getDeliveryMethod() {
		final var party = "11111111-1111-1111-1111-111111111111";

		assertThat(PrecheckUtil.getDeliveryMethod(null, emptyList(), emptyList())).isEqualTo(DELIVERY_NOT_POSSIBLE);
		assertThat(PrecheckUtil.getDeliveryMethod(party, List.of(party), emptyList())).isEqualTo(DIGITAL_MAIL);
		assertThat(PrecheckUtil.getDeliveryMethod(party, emptyList(), List.of(party))).isEqualTo(SNAIL_MAIL);
		assertThat(PrecheckUtil.getDeliveryMethod(party, emptyList(), emptyList())).isEqualTo(DELIVERY_NOT_POSSIBLE);
	}

	private PersonGuidBatch createPersonGuidBatch(boolean success) {
		final var personGuidBatch = new PersonGuidBatch();

		personGuidBatch.setSuccess(success);

		if (success) {
			personGuidBatch.setPersonId(UUID.randomUUID());
		}

		return personGuidBatch;
	}

	private Mailbox createMailbox(String partyId, Boolean reachable) {
		final var mailbox = new Mailbox();

		mailbox.setPartyId(partyId);
		mailbox.setReachable(reachable);

		return mailbox;
	}
}
