package se.sundsvall.postportalservice;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.postportalservice.Constants.ORIGIN;

class ConstantsTest {

	@Test
	void testOriginValue() {
		assertThat(ORIGIN).isEqualTo("PostPortalService");
	}
}
