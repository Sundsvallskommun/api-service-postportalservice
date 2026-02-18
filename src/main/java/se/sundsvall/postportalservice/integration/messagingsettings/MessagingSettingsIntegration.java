package se.sundsvall.postportalservice.integration.messagingsettings;

import generated.se.sundsvall.messagingsettings.MessagingSettingValue;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;
import se.sundsvall.dept44.support.Identifier;

import static org.zalando.problem.Status.BAD_GATEWAY;

@Component
public class MessagingSettingsIntegration {

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

	private final MessagingSettingsClient messagingSettingsClient;

	public MessagingSettingsIntegration(final MessagingSettingsClient messagingSettingsClient) {
		this.messagingSettingsClient = messagingSettingsClient;
	}

	public Map<String, String> getMessagingSettingsForUser(final String municipalityId) {
		var settings = messagingSettingsClient.getMessagingSettingsForUser(Identifier.get().toHeaderValue(), municipalityId);
		var user = Identifier.get().getValue();

		if (settings.isEmpty()) {
			throw Problem.valueOf(BAD_GATEWAY, "No messaging settings found for user '%s' in municipalityId '%s'".formatted(user, municipalityId));
		}

		if (settings.size() > 1) {
			throw Problem.valueOf(BAD_GATEWAY, "Found multiple messaging settings for user '%s' in municipalityId '%s', can't determine which one to use"
				.formatted(Identifier.get().getValue(), municipalityId));
		}

		var settingsMap = settings.getFirst().getValues().stream()
			.collect(Collectors.toMap(MessagingSettingValue::getKey, MessagingSettingValue::getValue));

		assertThatRequiredValuesArePresent(settingsMap, user, municipalityId);

		return settingsMap;
	}

	/**
	 * Asserts that all required values are present in the settings map.
	 *
	 * @param  settingsMap                          Settings map retrieved from MessagingSettings API
	 * @param  user                                 User identifier retrieved from X-Sent-By header
	 * @param  municipalityId                       Municipality ID for which the settings were requested
	 * @throws org.zalando.problem.ThrowableProblem if any expected value is missing
	 */
	private void assertThatRequiredValuesArePresent(final Map<String, String> settingsMap, final String user, final String municipalityId) {
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

}
