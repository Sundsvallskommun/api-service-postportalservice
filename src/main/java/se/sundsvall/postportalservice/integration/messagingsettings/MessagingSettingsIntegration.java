package se.sundsvall.postportalservice.integration.messagingsettings;

import static org.zalando.problem.Status.BAD_GATEWAY;

import generated.se.sundsvall.messagingsettings.SenderInfoResponse;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;

@Component
public class MessagingSettingsIntegration {

	private final MessagingSettingsClient messagingSettingsClient;

	public MessagingSettingsIntegration(final MessagingSettingsClient messagingSettingsClient) {
		this.messagingSettingsClient = messagingSettingsClient;
	}

	public Optional<String> getSupportText(final String municipalityId, final String departmentId) {
		return messagingSettingsClient.getSenderInfo(municipalityId, departmentId)
			.map(SenderInfoResponse::getSupportText);
	}

	public SenderInfoResponse getSenderInfo(final String municipalityId, final String departmentId) {
		return messagingSettingsClient.getSenderInfo(municipalityId, departmentId)
			.orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, "Found no sender info for departmentId " + departmentId));
	}
}
