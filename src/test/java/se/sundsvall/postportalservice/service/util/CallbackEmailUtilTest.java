package se.sundsvall.postportalservice.service.util;

import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CallbackEmailUtilTest {

	@Test
	void getCallbackEmail() {
		final var settingsMap = Map.of("callback_email", "test@example.com");
		assertThat(CallbackEmailUtil.getCallbackEmail(settingsMap)).isEqualTo("test@example.com");
	}

	@Test
	void getCallbackEmailSubject() {
		final var settingsMap = Map.of("callback_email_subject", "My Subject");
		assertThat(CallbackEmailUtil.getCallbackEmailSubject(settingsMap)).isEqualTo("My Subject");
	}

	@Test
	void getEmailBody() {
		final var settingsMap = Map.of("callback_email_body_base64", "PHA+Qm9keTwvcD4=");
		assertThat(CallbackEmailUtil.getEmailBody(settingsMap)).isEqualTo("PHA+Qm9keTwvcD4=");
	}

	@Test
	void returnsNullForMissingKeys() {
		final var settingsMap = Map.<String, String>of();
		assertThat(CallbackEmailUtil.getCallbackEmail(settingsMap)).isNull();
		assertThat(CallbackEmailUtil.getCallbackEmailSubject(settingsMap)).isNull();
		assertThat(CallbackEmailUtil.getEmailBody(settingsMap)).isNull();
	}
}
