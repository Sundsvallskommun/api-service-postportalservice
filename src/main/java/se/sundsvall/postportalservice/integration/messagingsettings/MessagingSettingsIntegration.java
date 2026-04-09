package se.sundsvall.postportalservice.integration.messagingsettings;

import generated.se.sundsvall.messagingsettings.MessagingSettingValue;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.support.Identifier;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.validateMessagingSettings;

@Component
public class MessagingSettingsIntegration {

	private final MessagingSettingsClient messagingSettingsClient;

	public MessagingSettingsIntegration(final MessagingSettingsClient messagingSettingsClient) {
		this.messagingSettingsClient = messagingSettingsClient;
	}

	public Map<String, String> getMessagingSettingsForUser(final String municipalityId) {
		return getMessagingSettingsForUser(municipalityId, Identifier.get().toHeaderValue());
	}

	public Map<String, String> getMessagingSettingsForUser(final String municipalityId, final String identifier) {
		var settings = messagingSettingsClient.getMessagingSettingsForUser(identifier, municipalityId);

		// Basic sanity checks
		if (settings.isEmpty()) {
			throw Problem.valueOf(BAD_GATEWAY, "No messaging settings found for user '%s' in municipalityId '%s'".formatted(identifier, municipalityId));
		}

		if (settings.size() > 1) {
			throw Problem.valueOf(BAD_GATEWAY, "Found multiple messaging settings for user '%s' in municipalityId '%s', can't determine which one to use"
				.formatted(identifier, municipalityId));
		}

		var settingsMap = settings.getFirst().getValues().stream()
			.filter(setting -> setting.getValue() != null)
			.collect(Collectors.toMap(MessagingSettingValue::getKey, MessagingSettingValue::getValue));

		validateMessagingSettings(settingsMap, identifier, municipalityId);

		return settingsMap;
	}

}
