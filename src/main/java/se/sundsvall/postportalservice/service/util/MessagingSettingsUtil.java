package se.sundsvall.postportalservice.service.util;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static io.micrometer.common.util.StringUtils.isBlank;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;

public class MessagingSettingsUtil {

	private MessagingSettingsUtil() {}

	// Constants for key names in MessagingSettings
	public static final String ORGANIZATION_NUMBER = "organization_number";
	public static final String FOLDER_NAME = "folder_name";
	public static final String SMS_SENDER = "sms_sender";
	public static final String SUPPORT_TEXT = "support_text";
	public static final String CONTACT_INFORMATION_URL = "contact_information_url";
	public static final String CONTACT_INFORMATION_PHONE_NUMBER = "contact_information_phone_number";
	public static final String CONTACT_INFORMATION_EMAIL = "contact_information_email";
	public static final String DEPARTMENT_NAME = "department_name";
	public static final String DEPARTMENT_ID = "department_id";

	// Constants for handling callback email when we cannot send snail mail
	public static final String SNAILMAIL_METHOD = "snailmail_method";
	public static final String SNAILMAIL_METHOD_VALUE = "Callback_Email";
	public static final String SNAILMAIL_CALLBACK_EMAIL = "callback_email";
	public static final String SNAILMAIL_CALLBACK_SUBJECT = "callback_email_subject";

	public static final List<String> REQUIRED_KEYS = List.of(
		ORGANIZATION_NUMBER,
		FOLDER_NAME,
		SMS_SENDER,
		SUPPORT_TEXT,
		CONTACT_INFORMATION_URL,
		CONTACT_INFORMATION_PHONE_NUMBER,
		CONTACT_INFORMATION_EMAIL,
		DEPARTMENT_NAME,
		DEPARTMENT_ID);

	public static final Map<String, String> VALUE_VALIDATION_MAP = Map.of(
		ORGANIZATION_NUMBER, "^\\d+$");

	public static final String ERROR_MESSAGE_ATTRIBUTE_MISSING = "Required messaging setting attribute '%s' is missing for user '%s' in municipalityId '%s'";

	public static void validateMessagingSettings(final Map<String, String> settingsMap, final String user, final String municipalityId) {
		assertThatRequiredValuesArePresent(settingsMap, user, municipalityId);
		assertRequiredValuesForEmailCallback(settingsMap, user, municipalityId);
	}

	/**
	 * Asserts that all required values are present in the settings map.
	 *
	 * @param  settingsMap      Settings map retrieved from MessagingSettings API
	 * @param  user             User identifier retrieved from X-Sent-By header
	 * @param  municipalityId   Municipality ID for which the settings were requested
	 * @throws ThrowableProblem if any expected value is missing
	 */
	private static void assertThatRequiredValuesArePresent(final Map<String, String> settingsMap, final String user, final String municipalityId) {
		// Iterate over required keys and check presence
		for (var key : REQUIRED_KEYS) {
			Optional.ofNullable(settingsMap.get(key))
				.orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, ERROR_MESSAGE_ATTRIBUTE_MISSING.formatted(key, user, municipalityId)));
		}

		// Validate specific values against their regex patterns
		for (var value : VALUE_VALIDATION_MAP.entrySet()) {
			var settingValue = settingsMap.get(value.getKey());
			if (!settingValue.matches(value.getValue())) {
				throw Problem.valueOf(BAD_GATEWAY, "Invalid format for messaging setting attribute '%s' for user '%s' in municipalityId '%s'".formatted(value.getKey(), user, municipalityId));
			}
		}
	}

	/**
	 * Asserts that required values for callback email are present (when snail-mail should not be sent)
	 *
	 * @param  settingsMap      Settings map retrieved from MessagingSettings API
	 * @param  user             User identifier retrieved from X-Sent-By header
	 * @param  municipalityId   Municipality ID for which the settings were requested
	 * @throws ThrowableProblem if any expected value is missing
	 */
	private static void assertRequiredValuesForEmailCallback(final Map<String, String> settingsMap, final String user, final String municipalityId) {
		// Check if there's a snailmail_method configured and that the snailmail_method == Callback_EMail
		if (settingsMap.containsKey(SNAILMAIL_METHOD) && SNAILMAIL_METHOD_VALUE.equalsIgnoreCase(settingsMap.get(SNAILMAIL_METHOD))) {
			// Verify required fields are present
			if (isBlank(settingsMap.get(SNAILMAIL_CALLBACK_EMAIL)) || isBlank(settingsMap.get(SNAILMAIL_CALLBACK_SUBJECT))) {
				throw Problem.valueOf(BAD_GATEWAY, "Missing required parameter(s) for callback email for user '%s' in municipalityId '%s'.".formatted(user, municipalityId));
			}
		}
	}
}
