package se.sundsvall.postportalservice.service.util;

import org.junit.jupiter.api.Test;
import se.sundsvall.dept44.support.Identifier;

import static org.assertj.core.api.Assertions.assertThat;

class IdentifierUtilTest {

	@Test
	void getIdentifierHeaderValue() {
		final var headerValue = IdentifierUtil.getIdentifierHeaderValue("joe01doe");

		// Round-trip through Identifier.parse to avoid coupling to the exact header serialization format.
		final var identifier = Identifier.parse(headerValue);
		assertThat(identifier).isNotNull();
		assertThat(identifier.getType()).isEqualTo(Identifier.Type.AD_ACCOUNT);
		assertThat(identifier.getValue()).isEqualTo("joe01doe");
	}
}
