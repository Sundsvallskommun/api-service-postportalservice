package se.sundsvall.postportalservice.integration.messagingsettings;

import static org.zalando.problem.Status.BAD_GATEWAY;
import static org.zalando.problem.Status.NOT_FOUND;

import generated.se.sundsvall.messagingsettings.MessagingSettingValue;
import generated.se.sundsvall.messagingsettings.SenderInfoResponse;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;
import se.sundsvall.dept44.support.Identifier;

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
			throw Problem.valueOf(BAD_GATEWAY, "Found multiple messaging settings for user '%s' in municipalityId '%s', can't determine which is should be used."
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
	 * @param settingsMap Settings map retrieved from MessagingSettings API
	 * @param user User identifier retrieved from X-Sent-By header
	 * @param municipalityId Municipality ID for which the settings were requested
	 * @throws org.zalando.problem.ThrowableProblem if any expected value is missing
	 */
	private void assertThatRequiredValuesArePresent(final Map<String, String> settingsMap, final String user, final String municipalityId) {
		Optional.ofNullable(settingsMap.get(ORGANIZATION_NUMBER))
			.orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, ERROR_MESSAGE_ATTRIBUTE_MISSING.formatted(ORGANIZATION_NUMBER, user, municipalityId)));

		Optional.ofNullable(settingsMap.get(FOLDER_NAME))
			.orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, ERROR_MESSAGE_ATTRIBUTE_MISSING.formatted(FOLDER_NAME, user, municipalityId)));

		Optional.ofNullable(settingsMap.get(SMS_SENDER))
			.orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, ERROR_MESSAGE_ATTRIBUTE_MISSING.formatted(SMS_SENDER, user, municipalityId)));

		Optional.ofNullable(settingsMap.get(SUPPORT_TEXT))
			.orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, ERROR_MESSAGE_ATTRIBUTE_MISSING.formatted(SUPPORT_TEXT, user, municipalityId)));

		Optional.ofNullable(settingsMap.get(CONTACT_INFORMATION_URL))
			.orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, ERROR_MESSAGE_ATTRIBUTE_MISSING.formatted(CONTACT_INFORMATION_URL, user, municipalityId)));

		Optional.ofNullable(settingsMap.get(CONTACT_INFORMATION_PHONE_NUMBER))
			.orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, ERROR_MESSAGE_ATTRIBUTE_MISSING.formatted(CONTACT_INFORMATION_PHONE_NUMBER, user, municipalityId)));

		Optional.ofNullable(settingsMap.get(CONTACT_INFORMATION_EMAIL))
			.orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, ERROR_MESSAGE_ATTRIBUTE_MISSING.formatted(CONTACT_INFORMATION_EMAIL, user, municipalityId)));

		Optional.ofNullable(settingsMap.get(DEPARTMENT_NAME))
			.orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, ERROR_MESSAGE_ATTRIBUTE_MISSING.formatted(DEPARTMENT_NAME, user, municipalityId)));

		Optional.ofNullable(settingsMap.get(DEPARTMENT_ID))
			.orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, ERROR_MESSAGE_ATTRIBUTE_MISSING.formatted(DEPARTMENT_ID, user, municipalityId)));
	}

	public SenderInfoResponse getSenderInfo(final String municipalityId, final String departmentId) {
		return messagingSettingsClient.getSenderInfo(municipalityId, departmentId)
			.stream()
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, "Found no sender info for departmentId " + departmentId));
	}

	public String getOrganizationNumber(final String municipalityId, final String departmentId) {
		return messagingSettingsClient.getSenderInfo(municipalityId, departmentId)
			.stream()
			.findFirst()
			.map(SenderInfoResponse::getOrganizationNumber)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "Organization number not found for municipalityId '%s' and departmentId '%s'".formatted(municipalityId, departmentId)));
	}

}
