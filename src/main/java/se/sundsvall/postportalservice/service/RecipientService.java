package se.sundsvall.postportalservice.service;

import org.springframework.stereotype.Service;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.postportalservice.api.model.RecipientResponse;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.dao.MessageRepository;
import se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration;
import se.sundsvall.postportalservice.service.mapper.RecipientMapper;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class RecipientService {

	private final MessageRepository messageRepository;
	private final MessagingSettingsIntegration messagingSettingsIntegration;
	private final RecipientMapper recipientMapper;

	public RecipientService(final MessageRepository messageRepository, final MessagingSettingsIntegration messagingSettingsIntegration, final RecipientMapper recipientMapper) {
		this.messageRepository = messageRepository;
		this.messagingSettingsIntegration = messagingSettingsIntegration;
		this.recipientMapper = recipientMapper;
	}

	public RecipientResponse getRecipient(final String municipalityId, final String recipientId) {
		final var message = getMessageByRecipientId(municipalityId, recipientId);
		final var recipient = message.getRecipients().stream()
			.filter(r -> r.getId().equals(recipientId))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "Recipient with id '%s' not found".formatted(recipientId)));

		final var settingsMap = messagingSettingsIntegration.getMessagingSettingsForUser(municipalityId, message.getUser().getUsername());

		return recipientMapper.toRecipientResponse(message, recipient, settingsMap);
	}

	private MessageEntity getMessageByRecipientId(final String municipalityId, final String recipientId) {
		return messageRepository.findByMunicipalityIdAndRecipients_Id(municipalityId, recipientId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No message found for recipient with id '%s' in municipality '%s'".formatted(recipientId, municipalityId)));
	}
}
