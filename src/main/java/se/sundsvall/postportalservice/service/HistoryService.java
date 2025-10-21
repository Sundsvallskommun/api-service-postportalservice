package se.sundsvall.postportalservice.service;

import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.postportalservice.integration.db.converter.MessageType.DIGITAL_REGISTERED_LETTER;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;
import se.sundsvall.dept44.models.api.paging.PagingAndSortingMetaData;
import se.sundsvall.postportalservice.api.model.Message;
import se.sundsvall.postportalservice.api.model.MessageDetails;
import se.sundsvall.postportalservice.api.model.Messages;
import se.sundsvall.postportalservice.api.model.SigningInformation;
import se.sundsvall.postportalservice.integration.db.dao.MessageRepository;
import se.sundsvall.postportalservice.integration.digitalregisteredletter.DigitalRegisteredLetterIntegration;
import se.sundsvall.postportalservice.service.mapper.HistoryMapper;

@Service
public class HistoryService {
	private final DigitalRegisteredLetterIntegration digitalRegisteredLetterIntegration;
	private final MessageRepository messageRepository;
	private final HistoryMapper historyMapper;

	public HistoryService(
		final DigitalRegisteredLetterIntegration digitalRegisteredLetterIntegration,
		final MessageRepository messageRepository,
		final HistoryMapper historyMapper) {
		this.digitalRegisteredLetterIntegration = digitalRegisteredLetterIntegration;
		this.messageRepository = messageRepository;
		this.historyMapper = historyMapper;
	}

	public Messages getUserMessages(final String municipalityId, final String username, final Pageable pageable) {
		final var page = messageRepository.findAllByMunicipalityIdAndUserUsernameIgnoreCase(municipalityId, username, pageable);
		final var messages = historyMapper.toMessageList(page.getContent());

		decorateWithSigningInformation(municipalityId, messages);

		return Messages.create()
			.withMetaData(PagingAndSortingMetaData.create().withPageData(page))
			.withMessages(messages);
	}

	private void decorateWithSigningInformation(final String municipalityId, final List<Message> messages) {
		// Filter out ids for all correspondence that has been sent as digital registered letters
		final var digitalRegisteredLetterIds = messages.stream()
			.filter(message -> DIGITAL_REGISTERED_LETTER.name().equals(message.getType()))
			.map(Message::getMessageId)
			.toList();

		// Fetch signing status for the ids that has been found
		digitalRegisteredLetterIntegration.getLetterStatuses(municipalityId, digitalRegisteredLetterIds).stream()
			.forEach(letterStatus -> messages.stream()
				.filter(message -> message.getMessageId().equals(letterStatus.getLetterId()))
				.findFirst()
				.ifPresent(message -> message.setSigningStatus(historyMapper.toSigningStatus(letterStatus))));
	}

	public MessageDetails getMessageDetails(final String municipalityId, final String username, final String messageId) {
		return messageRepository.findByMunicipalityIdAndIdAndUserUsernameIgnoreCase(municipalityId, messageId, username)
			.map(historyMapper::toMessageDetails)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "Message with id '%s' and municipalityId '%s' for user with username '%s' not found".formatted(messageId, municipalityId, username)));
	}

	public SigningInformation getSigningInformation(final String municipalityId, final String messageId) {
		final var message = messageRepository.findByIdAndMessageType(messageId, DIGITAL_REGISTERED_LETTER)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No digital registered letter found for id '%s'".formatted(messageId)));

		// Digital registered letters are always sent to a single recipient
		final var externalId = message.getRecipients().getFirst().getExternalId();

		return digitalRegisteredLetterIntegration.getSigningInformation(municipalityId, externalId);
	}

}
