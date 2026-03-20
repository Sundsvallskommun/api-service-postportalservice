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
		var settings = messagingSettingsClient.getMessagingSettingsForUser(Identifier.get().toHeaderValue(), municipalityId);
		var user = Identifier.get().getValue();

		// Basic sanity checks
		if (settings.isEmpty()) {
			throw Problem.valueOf(BAD_GATEWAY, "No messaging settings found for user '%s' in municipalityId '%s'".formatted(user, municipalityId));
		}

		if (settings.size() > 1) {
			throw Problem.valueOf(BAD_GATEWAY, "Found multiple messaging settings for user '%s' in municipalityId '%s', can't determine which one to use"
				.formatted(Identifier.get().getValue(), municipalityId));
		}

		var settingsMap = settings.getFirst().getValues().stream()
			.filter(setting -> setting.getValue() != null)
			.collect(Collectors.toMap(MessagingSettingValue::getKey, MessagingSettingValue::getValue));

		validateMessagingSettings(settingsMap, user, municipalityId);

		return settingsMap;
	}

}
