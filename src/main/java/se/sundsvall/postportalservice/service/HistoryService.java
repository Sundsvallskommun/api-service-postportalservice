package se.sundsvall.postportalservice.service;

import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.postportalservice.integration.db.converter.MessageType.DIGITAL_REGISTERED_LETTER;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.zalando.problem.Problem;
import se.sundsvall.dept44.models.api.paging.PagingAndSortingMetaData;
import se.sundsvall.postportalservice.api.model.Message;
import se.sundsvall.postportalservice.api.model.MessageDetails;
import se.sundsvall.postportalservice.api.model.Messages;
import se.sundsvall.postportalservice.api.model.SigningInformation;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
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

		final var messages = decorateWithSigningInformation(municipalityId, page);

		return Messages.create()
			.withMetaData(PagingAndSortingMetaData.create().withPageData(page))
			.withMessages(messages);
	}

	private List<Message> decorateWithSigningInformation(final String municipalityId, final Page<MessageEntity> page) {
		final var messages = historyMapper.toMessageList(page.getContent());
		final var messageById = messages.stream()
			.collect(Collectors.toMap(Message::getMessageId, m -> m));

		final var letterIdToMessageMap = page.getContent().stream()
			.filter(entity -> DIGITAL_REGISTERED_LETTER.equals(entity.getMessageType()))
			.map(entity -> Map.entry(entity.getRecipients().getFirst().getExternalId(), entity.getId()))
			.filter(entry -> entry.getKey() != null)
			.collect(Collectors.toMap(
				Map.Entry::getKey,
				entry -> messageById.get(entry.getValue()),
				(existing, ignored) -> existing,
				LinkedHashMap::new));

		if (!letterIdToMessageMap.isEmpty()) {
			final var letterIds = List.copyOf(letterIdToMessageMap.keySet());
			digitalRegisteredLetterIntegration.getLetterStatuses(municipalityId, letterIds)
				.forEach(status -> {
					final var message = letterIdToMessageMap.get(status.getLetterId());
					if (message != null)
						message.setSigningStatus(historyMapper.toSigningStatus(status));
				});
		}

		return messages;
	}

	public MessageDetails getMessageDetails(final String municipalityId, final String username, final String messageId) {
		final var messageEntity = messageRepository.findByMunicipalityIdAndIdAndUserUsernameIgnoreCase(municipalityId, messageId, username)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "Message with id '%s' and municipalityId '%s' for user with username '%s' not found".formatted(messageId, municipalityId, username)));

		final var messageDetails = historyMapper.toMessageDetails(messageEntity);

		// Decorate with signing information if this is a digital registered letter
		if (DIGITAL_REGISTERED_LETTER.equals(messageEntity.getMessageType())) {
			final var letterId = getLetterIdFromMessage(messageEntity);
			final var letterStatuses = digitalRegisteredLetterIntegration.getLetterStatuses(municipalityId, List.of(letterId));
			letterStatuses.stream()
				.filter(letterStatus -> letterId.equals(letterStatus.getLetterId()))
				.findFirst()
				.ifPresent(letterStatus -> messageDetails.setSigningStatus(historyMapper.toSigningStatus(letterStatus)));
		}

		return messageDetails;
	}

	public SigningInformation getSigningInformation(final String municipalityId, final String messageId) {
		final var message = getDigitalRegisteredLetterMessage(messageId);
		final var letterId = getLetterIdFromMessage(message);

		return digitalRegisteredLetterIntegration.getSigningInformation(municipalityId, letterId);
	}

	public ResponseEntity<StreamingResponseBody> getLetterReceipt(final String municipalityId, final String messageId) {
		final var message = getDigitalRegisteredLetterMessage(messageId);
		final var letterId = getLetterIdFromMessage(message);

		return digitalRegisteredLetterIntegration.getLetterReceipt(municipalityId, letterId);
	}

	private MessageEntity getDigitalRegisteredLetterMessage(final String messageId) {
		return messageRepository.findByIdAndMessageType(messageId, DIGITAL_REGISTERED_LETTER)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No digital registered letter found for id '%s'".formatted(messageId)));
	}

	private String getLetterIdFromMessage(final MessageEntity message) {
		// Digital registered letters are always sent to a single recipient
		return message.getRecipients().getFirst().getExternalId();
	}

}
