package se.sundsvall.postportalservice.api.model;

import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.postportalservice.api.model.PrecheckResponse.PrecheckRecipient;

import static org.assertj.core.api.Assertions.assertThat;

class PrecheckResponseTest {

	@Test
	void ofFactoryAndAccessor() {
		final var recipient = new PrecheckRecipient("19111111-1111", "da03b33e-9de2-45ac-8291-31a88de59410", "DIGITAL_MAIL", null);

		final var response = PrecheckResponse.of(List.of(recipient));

		assertThat(response.precheckRecipients()).containsExactly(recipient);
	}

	@Test
	void precheckRecipientAccessors() {
		final var recipient = new PrecheckRecipient("19111111-1111", "partyId", "DELIVERY_NOT_POSSIBLE", "Person not found");

		assertThat(recipient.personalIdentityNumber()).isEqualTo("19111111-1111");
		assertThat(recipient.partyId()).isEqualTo("partyId");
		assertThat(recipient.deliveryMethod()).isEqualTo("DELIVERY_NOT_POSSIBLE");
		assertThat(recipient.reason()).isEqualTo("Person not found");
	}
}
