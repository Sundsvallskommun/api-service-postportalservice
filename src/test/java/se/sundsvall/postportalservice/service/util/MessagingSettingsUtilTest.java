package se.sundsvall.postportalservice.service.util;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import se.sundsvall.dept44.problem.Problem;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.CONTACT_INFORMATION_EMAIL;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.CONTACT_INFORMATION_PHONE_NUMBER;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.CONTACT_INFORMATION_URL;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.DEPARTMENT_ID;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.DEPARTMENT_NAME;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.FOLDER_NAME;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.ORGANIZATION_NUMBER;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.SMS_SENDER;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.SNAILMAIL_CALLBACK_EMAIL;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.SNAILMAIL_CALLBACK_SUBJECT;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.SNAILMAIL_METHOD;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.SNAILMAIL_METHOD_VALUE;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.SUPPORT_TEXT;

class MessagingSettingsUtilTest {

	private static final String USER = "testuser";
	private static final String MUNICIPALITY_ID = "2281";

	@Test
	void validateMessagingSettings_success() {
		final var settingsMap = createValidSettingsMap();

		assertThatNoException().isThrownBy(() -> MessagingSettingsUtil.validateMessagingSettings(settingsMap, USER, MUNICIPALITY_ID));
	}

	@Test
	void validateMessagingSettings_successWithCallbackEmail() {
		final var settingsMap = createValidSettingsMap();
		settingsMap.put(SNAILMAIL_METHOD, SNAILMAIL_METHOD_VALUE);
		settingsMap.put(SNAILMAIL_CALLBACK_EMAIL, "callback@example.com");
		settingsMap.put(SNAILMAIL_CALLBACK_SUBJECT, "Subject");

		assertThatNoException().isThrownBy(() -> MessagingSettingsUtil.validateMessagingSettings(settingsMap, USER, MUNICIPALITY_ID));
	}

	@Test
	void validateMessagingSettings_missingRequiredKey() {
		final var settingsMap = createValidSettingsMap();
		settingsMap.remove(DEPARTMENT_NAME);

		assertThatThrownBy(() -> MessagingSettingsUtil.validateMessagingSettings(settingsMap, USER, MUNICIPALITY_ID))
			.isInstanceOf(Problem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY)
			.hasMessage("Bad Gateway: Required messaging setting attribute '%s' is missing for user '%s' in municipalityId '%s'"
				.formatted(DEPARTMENT_NAME, USER, MUNICIPALITY_ID));
	}

	@Test
	void validateMessagingSettings_invalidOrganizationNumber() {
		final var settingsMap = createValidSettingsMap();
		settingsMap.put(ORGANIZATION_NUMBER, "ABC-123");

		assertThatThrownBy(() -> MessagingSettingsUtil.validateMessagingSettings(settingsMap, USER, MUNICIPALITY_ID))
			.isInstanceOf(Problem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY)
			.hasMessage("Bad Gateway: Invalid format for messaging setting attribute '%s' for user '%s' in municipalityId '%s'"
				.formatted(ORGANIZATION_NUMBER, USER, MUNICIPALITY_ID));
	}

	@Test
	void validateMessagingSettings_callbackEmailMissingEmail() {
		final var settingsMap = createValidSettingsMap();
		settingsMap.put(SNAILMAIL_METHOD, SNAILMAIL_METHOD_VALUE);
		settingsMap.put(SNAILMAIL_CALLBACK_SUBJECT, "Subject");

		assertThatThrownBy(() -> MessagingSettingsUtil.validateMessagingSettings(settingsMap, USER, MUNICIPALITY_ID))
			.isInstanceOf(Problem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY)
			.hasMessage("Bad Gateway: Missing required parameter(s) for callback email for user '%s' in municipalityId '%s'."
				.formatted(USER, MUNICIPALITY_ID));
	}

	@Test
	void validateMessagingSettings_callbackEmailMissingSubject() {
		final var settingsMap = createValidSettingsMap();
		settingsMap.put(SNAILMAIL_METHOD, SNAILMAIL_METHOD_VALUE);
		settingsMap.put(SNAILMAIL_CALLBACK_EMAIL, "callback@example.com");

		assertThatThrownBy(() -> MessagingSettingsUtil.validateMessagingSettings(settingsMap, USER, MUNICIPALITY_ID))
			.isInstanceOf(Problem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY)
			.hasMessage("Bad Gateway: Missing required parameter(s) for callback email for user '%s' in municipalityId '%s'."
				.formatted(USER, MUNICIPALITY_ID));
	}

	@Test
	void validateMessagingSettings_callbackEmailWithBlankValues() {
		final var settingsMap = createValidSettingsMap();
		settingsMap.put(SNAILMAIL_METHOD, SNAILMAIL_METHOD_VALUE);
		settingsMap.put(SNAILMAIL_CALLBACK_EMAIL, "");
		settingsMap.put(SNAILMAIL_CALLBACK_SUBJECT, "");

		assertThatThrownBy(() -> MessagingSettingsUtil.validateMessagingSettings(settingsMap, USER, MUNICIPALITY_ID))
			.isInstanceOf(Problem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY)
			.hasMessage("Bad Gateway: Missing required parameter(s) for callback email for user '%s' in municipalityId '%s'."
				.formatted(USER, MUNICIPALITY_ID));
	}

	@Test
	void validateMessagingSettings_snailmailMethodNotCallbackEmail() {
		final var settingsMap = createValidSettingsMap();
		settingsMap.put(SNAILMAIL_METHOD, "SomeOtherMethod");

		assertThatNoException().isThrownBy(() -> MessagingSettingsUtil.validateMessagingSettings(settingsMap, USER, MUNICIPALITY_ID));
	}

	@Test
	void validateMessagingSettings_callbackEmailCaseInsensitive() {
		final var settingsMap = createValidSettingsMap();
		settingsMap.put(SNAILMAIL_METHOD, "callback_email");
		settingsMap.put(SNAILMAIL_CALLBACK_EMAIL, "callback@example.com");
		settingsMap.put(SNAILMAIL_CALLBACK_SUBJECT, "Subject");

		assertThatNoException().isThrownBy(() -> MessagingSettingsUtil.validateMessagingSettings(settingsMap, USER, MUNICIPALITY_ID));
	}

	private Map<String, String> createValidSettingsMap() {
		final var map = new HashMap<String, String>();
		map.put(ORGANIZATION_NUMBER, "1234567890");
		map.put(FOLDER_NAME, "TestFolder");
		map.put(SMS_SENDER, "Sundsvall");
		map.put(SUPPORT_TEXT, "Support text");
		map.put(CONTACT_INFORMATION_URL, "https://example.com");
		map.put(CONTACT_INFORMATION_PHONE_NUMBER, "0123456789");
		map.put(CONTACT_INFORMATION_EMAIL, "test@example.com");
		map.put(DEPARTMENT_NAME, "Department 44");
		map.put(DEPARTMENT_ID, "dept44");
		return map;
	}
}
