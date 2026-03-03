package se.sundsvall.postportalservice.service;

import generated.se.sundsvall.digitalregisteredletter.LetterStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.postportalservice.api.model.SigningInformation;
import se.sundsvall.postportalservice.api.model.SigningStatus;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.db.converter.MessageType;
import se.sundsvall.postportalservice.integration.db.dao.MessageRepository;
import se.sundsvall.postportalservice.integration.digitalregisteredletter.DigitalRegisteredLetterIntegration;
import se.sundsvall.postportalservice.integration.party.PartyIntegration;
import se.sundsvall.postportalservice.service.mapper.HistoryMapper;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.ResponseEntity.ok;
import static se.sundsvall.postportalservice.TestDataFactory.MUNICIPALITY_ID;
import static se.sundsvall.postportalservice.integration.db.converter.MessageType.DIGITAL_REGISTERED_LETTER;

@ExtendWith(MockitoExtension.class)
class HistoryServiceTest {

	@Mock
	private Page<MessageEntity> pageMock;

	@Mock
	private MessageRepository messageRepositoryMock;

	@Mock(answer = Answers.CALLS_REAL_METHODS)
	private HistoryMapper historyMapperMock;

	@Mock
	private DigitalRegisteredLetterIntegration digitalRegisteredLetterIntegrationMock;

	@Mock
	private PartyIntegration partyIntegrationMock;

	@InjectMocks
	private HistoryService historyService;

	@AfterEach
	void ensureNoUnexpectedMockInteractions() {
		verifyNoMoreInteractions(messageRepositoryMock, pageMock, historyMapperMock, digitalRegisteredLetterIntegrationMock, partyIntegrationMock);
	}

	@ParameterizedTest
	@EnumSource(value = MessageType.class, mode = EXCLUDE, names = "DIGITAL_REGISTERED_LETTER")
	void getUserMessages_noDigitalRegisteredLettersCommunication(final MessageType messageType) {
		final var username = "username";
		final var messageEntity = MessageEntity.create()
			.withCreated(OffsetDateTime.now())
			.withSubject("subject")
			.withMessageType(messageType)
			.withId("id");
		final var messageEntities = List.of(messageEntity);

		when(messageRepositoryMock.findAllByMunicipalityIdAndUserUsernameIgnoreCase(eq(MUNICIPALITY_ID), eq(username), any(Pageable.class))).thenReturn(pageMock);
		when(pageMock.getContent()).thenReturn(messageEntities);
		when(pageMock.getSort()).thenReturn(Sort.unsorted());
		when(pageMock.getSize()).thenReturn(1);
		when(pageMock.getNumber()).thenReturn(0);
		when(pageMock.getNumberOfElements()).thenReturn(1);
		when(pageMock.getTotalElements()).thenReturn(1L);
		when(pageMock.getTotalPages()).thenReturn(1);

		final var messages = historyService.getUserMessages(MUNICIPALITY_ID, username, Pageable.unpaged());

		assertThat(messages).isNotNull().satisfies(messages1 -> {
			assertThat(messages1.getMessages()).allSatisfy(message -> {
				assertThat(message.getMessageId()).isEqualTo(messageEntity.getId());
				assertThat(message.getSubject()).isEqualTo(messageEntity.getSubject());
				assertThat(message.getSentAt()).isEqualTo(messageEntity.getCreated().toLocalDateTime());
				assertThat(message.getType()).isEqualTo(messageEntity.getMessageType().toString());
				assertThat(message.getSigningStatus()).isNull();
			});
			assertThat(messages1.getMetaData()).satisfies(metaData -> {
				assertThat(metaData.getPage()).isEqualTo(1);
				assertThat(metaData.getLimit()).isEqualTo(1);
				assertThat(metaData.getCount()).isEqualTo(1);
				assertThat(metaData.getTotalRecords()).isEqualTo(1);
				assertThat(metaData.getTotalPages()).isEqualTo(1);
			});
		});
		verify(messageRepositoryMock).findAllByMunicipalityIdAndUserUsernameIgnoreCase(eq(MUNICIPALITY_ID), eq(username), any(Pageable.class));
		verify(historyMapperMock).toMessageList(messageEntities);
		verify(historyMapperMock).toMessage(messageEntity);
		verify(pageMock, times(2)).getContent();
	}

	@Test
	void getUserMessages_digitalRegisteredLettersCommunicationWithMatchingLetterStatus() {
		final var username = "username";
		final var messageId = "messageId";
		final var letterId = "letterId123";
		final var letterState = "letterState";
		final var signingProcessState = "signingProcessState";
		final var recipientEntity = new RecipientEntity().withExternalId(letterId);
		final var messageEntity = MessageEntity.create()
			.withCreated(OffsetDateTime.now())
			.withSubject("subject")
			.withMessageType(MessageType.DIGITAL_REGISTERED_LETTER)
			.withId(messageId)
			.withRecipients(List.of(recipientEntity));
		final var messageEntities = List.of(messageEntity);
		final var letterStatus = new LetterStatus()
			.letterId(letterId)
			.status(letterState);
		final var signingStatus = SigningStatus.create()
			.withLetterState(letterState)
			.withSigningProcessState(signingProcessState);

		when(messageRepositoryMock.findAllByMunicipalityIdAndUserUsernameIgnoreCase(eq(MUNICIPALITY_ID), eq(username), any(Pageable.class))).thenReturn(pageMock);
		when(digitalRegisteredLetterIntegrationMock.getLetterStatuses(MUNICIPALITY_ID, List.of(letterId))).thenReturn(List.of(letterStatus));
		when(historyMapperMock.toSigningStatus(letterStatus)).thenReturn(signingStatus);

		when(pageMock.getContent()).thenReturn(messageEntities);
		when(pageMock.getSort()).thenReturn(Sort.unsorted());
		when(pageMock.getSize()).thenReturn(1);
		when(pageMock.getNumber()).thenReturn(0);
		when(pageMock.getNumberOfElements()).thenReturn(1);
		when(pageMock.getTotalElements()).thenReturn(1L);
		when(pageMock.getTotalPages()).thenReturn(1);

		final var messages = historyService.getUserMessages(MUNICIPALITY_ID, username, Pageable.unpaged());

		assertThat(messages).isNotNull().satisfies(messages1 -> {
			assertThat(messages1.getMessages()).allSatisfy(message -> {
				assertThat(message.getMessageId()).isEqualTo(messageEntity.getId());
				assertThat(message.getSubject()).isEqualTo(messageEntity.getSubject());
				assertThat(message.getSentAt()).isEqualTo(messageEntity.getCreated().toLocalDateTime());
				assertThat(message.getType()).isEqualTo(messageEntity.getMessageType().toString());
				assertThat(message.getSigningStatus()).isNotNull().satisfies(messageSigningStatus -> {
					assertThat(messageSigningStatus.getLetterState()).isEqualTo(letterState);
					assertThat(messageSigningStatus.getSigningProcessState()).isEqualTo(signingProcessState);
				});
			});
			assertThat(messages1.getMetaData()).satisfies(metaData -> {
				assertThat(metaData.getPage()).isEqualTo(1);
				assertThat(metaData.getLimit()).isEqualTo(1);
				assertThat(metaData.getCount()).isEqualTo(1);
				assertThat(metaData.getTotalRecords()).isEqualTo(1);
				assertThat(metaData.getTotalPages()).isEqualTo(1);
			});
		});
		verify(messageRepositoryMock).findAllByMunicipalityIdAndUserUsernameIgnoreCase(eq(MUNICIPALITY_ID), eq(username), any(Pageable.class));
		verify(digitalRegisteredLetterIntegrationMock).getLetterStatuses(MUNICIPALITY_ID, List.of(letterId));
		verify(historyMapperMock).toMessageList(messageEntities);
		verify(historyMapperMock).toMessage(messageEntity);
		verify(historyMapperMock).toSigningStatus(letterStatus);
		verify(pageMock, times(2)).getContent();
	}

	@Test
	void getUserMessages_digitalRegisteredLettersCommunicationWithNonMatchingStatus() {
		final var username = "username";
		final var messageId = "messageId";
		final var letterId = "letterId123";
		final var status = "COMPLETED";
		final var recipientEntity = new RecipientEntity().withExternalId(letterId);
		final var messageEntity = MessageEntity.create()
			.withCreated(OffsetDateTime.now())
			.withSubject("subject")
			.withMessageType(MessageType.DIGITAL_REGISTERED_LETTER)
			.withId(messageId)
			.withRecipients(List.of(recipientEntity));
		final var messageEntities = List.of(messageEntity);
		final var letterStatus = new LetterStatus()
			.letterId("otherLetterId")
			.status(status);

		when(messageRepositoryMock.findAllByMunicipalityIdAndUserUsernameIgnoreCase(eq(MUNICIPALITY_ID), eq(username), any(Pageable.class))).thenReturn(pageMock);
		when(digitalRegisteredLetterIntegrationMock.getLetterStatuses(MUNICIPALITY_ID, List.of(letterId))).thenReturn(List.of(letterStatus));
		when(pageMock.getContent()).thenReturn(messageEntities);
		when(pageMock.getSort()).thenReturn(Sort.unsorted());
		when(pageMock.getSize()).thenReturn(1);
		when(pageMock.getNumber()).thenReturn(0);
		when(pageMock.getNumberOfElements()).thenReturn(1);
		when(pageMock.getTotalElements()).thenReturn(1L);
		when(pageMock.getTotalPages()).thenReturn(1);

		final var messages = historyService.getUserMessages(MUNICIPALITY_ID, username, Pageable.unpaged());

		assertThat(messages).isNotNull().satisfies(messages1 -> {
			assertThat(messages1.getMessages()).allSatisfy(message -> {
				assertThat(message.getMessageId()).isEqualTo(messageEntity.getId());
				assertThat(message.getSubject()).isEqualTo(messageEntity.getSubject());
				assertThat(message.getSentAt()).isEqualTo(messageEntity.getCreated().toLocalDateTime());
				assertThat(message.getType()).isEqualTo(messageEntity.getMessageType().toString());
				assertThat(message.getSigningStatus()).isNull();
			});
			assertThat(messages1.getMetaData()).satisfies(metaData -> {
				assertThat(metaData.getPage()).isEqualTo(1);
				assertThat(metaData.getLimit()).isEqualTo(1);
				assertThat(metaData.getCount()).isEqualTo(1);
				assertThat(metaData.getTotalRecords()).isEqualTo(1);
				assertThat(metaData.getTotalPages()).isEqualTo(1);
			});
		});
		verify(messageRepositoryMock).findAllByMunicipalityIdAndUserUsernameIgnoreCase(eq(MUNICIPALITY_ID), eq(username), any(Pageable.class));
		verify(digitalRegisteredLetterIntegrationMock).getLetterStatuses(MUNICIPALITY_ID, List.of(letterId));
		verify(historyMapperMock).toMessageList(messageEntities);
		verify(historyMapperMock).toMessage(messageEntity);
		verify(pageMock, times(2)).getContent();
	}

	private static Stream<Arguments> missingRecipientDataProvider() {
		return Stream.of(
			Arguments.of("recipients is null", null),
			Arguments.of("recipients is empty", List.of()),
			Arguments.of("externalId is null", List.of(new RecipientEntity().withExternalId(null))));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("missingRecipientDataProvider")
	void getUserMessages_digitalRegisteredLetterWithMissingRecipientData(final String description, final List<RecipientEntity> recipients) {
		final var username = "username";
		final var messageEntity = MessageEntity.create()
			.withCreated(OffsetDateTime.now())
			.withSubject("subject")
			.withMessageType(MessageType.DIGITAL_REGISTERED_LETTER)
			.withId("messageId")
			.withRecipients(recipients);
		final var messageEntities = List.of(messageEntity);

		when(messageRepositoryMock.findAllByMunicipalityIdAndUserUsernameIgnoreCase(eq(MUNICIPALITY_ID), eq(username), any(Pageable.class))).thenReturn(pageMock);
		when(pageMock.getContent()).thenReturn(messageEntities);
		when(pageMock.getSort()).thenReturn(Sort.unsorted());
		when(pageMock.getSize()).thenReturn(1);
		when(pageMock.getNumber()).thenReturn(0);
		when(pageMock.getNumberOfElements()).thenReturn(1);
		when(pageMock.getTotalElements()).thenReturn(1L);
		when(pageMock.getTotalPages()).thenReturn(1);

		final var messages = historyService.getUserMessages(MUNICIPALITY_ID, username, Pageable.unpaged());

		assertThat(messages).isNotNull().satisfies(result -> {
			assertThat(result.getMessages()).allSatisfy(message -> {
				assertThat(message.getMessageId()).isEqualTo(messageEntity.getId());
				assertThat(message.getSubject()).isEqualTo(messageEntity.getSubject());
				assertThat(message.getSentAt()).isEqualTo(messageEntity.getCreated().toLocalDateTime());
				assertThat(message.getType()).isEqualTo(messageEntity.getMessageType().toString());
				assertThat(message.getSigningStatus()).isNull();
			});
		});
		verify(messageRepositoryMock).findAllByMunicipalityIdAndUserUsernameIgnoreCase(eq(MUNICIPALITY_ID), eq(username), any(Pageable.class));
		verify(historyMapperMock).toMessageList(messageEntities);
		verify(historyMapperMock).toMessage(messageEntity);
		verify(pageMock, times(2)).getContent();
		verifyNoInteractions(digitalRegisteredLetterIntegrationMock);
	}

	@Test
	void getMessageDetails() {
		final var messageId = "messageId";
		final var userId = "userId";
		final var partyId = "partyId123";
		final var message = MessageEntity.create()
			.withSubject("subject")
			.withCreated(OffsetDateTime.now())
			.withAttachments(List.of())
			.withRecipients(List.of(new RecipientEntity().withPartyId(partyId)));

		when(messageRepositoryMock.findByMunicipalityIdAndIdAndUserUsernameIgnoreCase(MUNICIPALITY_ID, messageId, userId)).thenReturn(Optional.of(message));
		when(partyIntegrationMock.getLegalIds(MUNICIPALITY_ID, List.of(partyId))).thenReturn(Map.of(partyId, "legalId123"));

		final var result = historyService.getMessageDetails(MUNICIPALITY_ID, userId, messageId);

		assertThat(result).isNotNull().satisfies(messageDetails -> {
			assertThat(messageDetails.getSubject()).isEqualTo(message.getSubject());
			assertThat(messageDetails.getSentAt()).isEqualTo(message.getCreated().toLocalDateTime());
			assertThat(messageDetails.getAttachments()).isEmpty();
			assertThat(messageDetails.getRecipients()).hasSize(1).allSatisfy(recipient -> {
				assertThat(recipient.getPartyId()).isEqualTo(partyId);
				assertThat(recipient.getLegalId()).isEqualTo("legalId123");
			});
		});

		verify(messageRepositoryMock).findByMunicipalityIdAndIdAndUserUsernameIgnoreCase(MUNICIPALITY_ID, messageId, userId);
		verify(partyIntegrationMock).getLegalIds(MUNICIPALITY_ID, List.of(partyId));
		verify(historyMapperMock).toMessageDetails(message);
		verify(historyMapperMock).toAttachmentList(message.getAttachments());
		verify(historyMapperMock).toRecipientList(message.getRecipients());
		verify(historyMapperMock).toRecipient(message.getRecipients().getFirst());
	}

	@Test
	void getMessageDetailsNotFound() {
		final var messageId = "messageId";
		final var userId = "userId";

		when(messageRepositoryMock.findByMunicipalityIdAndIdAndUserUsernameIgnoreCase(MUNICIPALITY_ID, messageId, userId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> historyService.getMessageDetails(MUNICIPALITY_ID, userId, messageId))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("not found");

		verify(messageRepositoryMock).findByMunicipalityIdAndIdAndUserUsernameIgnoreCase(MUNICIPALITY_ID, messageId, userId);
	}

	@Test
	void getMessageDetails_withDigitalRegisteredLetterAndSigningStatus() {
		final var messageId = "messageId";
		final var letterId = "letterId123";
		final var partyId = "partyId123";
		final var userId = "userId";
		final var letterState = "letterState";
		final var signingProcessState = "signingProcessState";
		final var recipientEntity = new RecipientEntity().withPartyId(partyId).withExternalId(letterId);
		final var message = MessageEntity.create()
			.withSubject("subject")
			.withCreated(OffsetDateTime.now())
			.withAttachments(List.of())
			.withRecipients(List.of(recipientEntity))
			.withMessageType(DIGITAL_REGISTERED_LETTER)
			.withId(messageId);
		final var letterStatus = new LetterStatus()
			.letterId(letterId)
			.status(letterState);
		final var signingStatus = SigningStatus.create()
			.withLetterState(letterState)
			.withSigningProcessState(signingProcessState);

		when(messageRepositoryMock.findByMunicipalityIdAndIdAndUserUsernameIgnoreCase(MUNICIPALITY_ID, messageId, userId)).thenReturn(Optional.of(message));
		when(digitalRegisteredLetterIntegrationMock.getLetterStatuses(MUNICIPALITY_ID, List.of(letterId))).thenReturn(List.of(letterStatus));
		when(historyMapperMock.toSigningStatus(letterStatus)).thenReturn(signingStatus);

		final var result = historyService.getMessageDetails(MUNICIPALITY_ID, userId, messageId);

		assertThat(result).isNotNull().satisfies(messageDetails -> {
			assertThat(messageDetails.getSubject()).isEqualTo(message.getSubject());
			assertThat(messageDetails.getSentAt()).isEqualTo(message.getCreated().toLocalDateTime());
			assertThat(messageDetails.getAttachments()).isEmpty();
			assertThat(messageDetails.getRecipients()).hasSize(1);
			assertThat(messageDetails.getSigningStatus()).isNotNull().satisfies(status -> {
				assertThat(status.getLetterState()).isEqualTo(letterState);
				assertThat(status.getSigningProcessState()).isEqualTo(signingProcessState);
			});
		});
		verify(messageRepositoryMock).findByMunicipalityIdAndIdAndUserUsernameIgnoreCase(MUNICIPALITY_ID, messageId, userId);
		verify(digitalRegisteredLetterIntegrationMock).getLetterStatuses(MUNICIPALITY_ID, List.of(letterId));
		verify(partyIntegrationMock).getLegalIds(MUNICIPALITY_ID, List.of(partyId));
		verify(historyMapperMock).toMessageDetails(message);
		verify(historyMapperMock).toAttachmentList(message.getAttachments());
		verify(historyMapperMock).toRecipientList(message.getRecipients());
		verify(historyMapperMock).toRecipient(recipientEntity);
		verify(historyMapperMock).toSigningStatus(letterStatus);
	}

	@Test
	void getMessageDetails_withDigitalRegisteredLetterButNoSigningStatus() {
		final var messageId = "messageId";
		final var letterId = "letterId123";
		final var userId = "userId";
		final var partyId = "partyId123";
		final var recipientEntity = new RecipientEntity().withPartyId(partyId).withExternalId(letterId);
		final var message = MessageEntity.create()
			.withSubject("subject")
			.withCreated(OffsetDateTime.now())
			.withAttachments(List.of())
			.withRecipients(List.of(recipientEntity))
			.withMessageType(DIGITAL_REGISTERED_LETTER)
			.withId(messageId);

		when(messageRepositoryMock.findByMunicipalityIdAndIdAndUserUsernameIgnoreCase(MUNICIPALITY_ID, messageId, userId)).thenReturn(Optional.of(message));
		when(digitalRegisteredLetterIntegrationMock.getLetterStatuses(MUNICIPALITY_ID, List.of(letterId))).thenReturn(emptyList());

		final var result = historyService.getMessageDetails(MUNICIPALITY_ID, userId, messageId);

		assertThat(result).isNotNull().satisfies(messageDetails -> {
			assertThat(messageDetails.getSubject()).isEqualTo(message.getSubject());
			assertThat(messageDetails.getSentAt()).isEqualTo(message.getCreated().toLocalDateTime());
			assertThat(messageDetails.getAttachments()).isEmpty();
			assertThat(messageDetails.getRecipients()).hasSize(1);
			assertThat(messageDetails.getSigningStatus()).isNull();
		});
		verify(partyIntegrationMock).getLegalIds(MUNICIPALITY_ID, List.of(partyId));
		verify(messageRepositoryMock).findByMunicipalityIdAndIdAndUserUsernameIgnoreCase(MUNICIPALITY_ID, messageId, userId);
		verify(digitalRegisteredLetterIntegrationMock).getLetterStatuses(MUNICIPALITY_ID, List.of(letterId));
		verify(historyMapperMock).toMessageDetails(message);
		verify(historyMapperMock).toAttachmentList(message.getAttachments());
		verify(historyMapperMock).toRecipientList(message.getRecipients());
		verify(historyMapperMock).toRecipient(recipientEntity);
	}

	@Test
	void getMessageDetails_withMixOfRecipientsWithAndWithoutPartyIds() {
		final var messageId = "messageId";
		final var userId = "userId";
		final var partyId1 = "partyId1";
		final var partyId2 = "partyId2";
		// Recipient with partyId (SNAIL_MAIL/DIGITAL_MAIL)
		final var recipientWithPartyId1 = new RecipientEntity().withPartyId(partyId1);
		final var recipientWithPartyId2 = new RecipientEntity().withPartyId(partyId2);
		// Recipient without partyId (SMS)
		final var recipientWithoutPartyId = new RecipientEntity().withPartyId(null).withPhoneNumber("+46701234567");
		final var message = MessageEntity.create()
			.withSubject("subject")
			.withCreated(OffsetDateTime.now())
			.withAttachments(List.of())
			.withRecipients(List.of(recipientWithPartyId1, recipientWithoutPartyId, recipientWithPartyId2));

		when(messageRepositoryMock.findByMunicipalityIdAndIdAndUserUsernameIgnoreCase(MUNICIPALITY_ID, messageId, userId)).thenReturn(Optional.of(message));
		// Only partyIds that are non-null should be sent to party integration
		when(partyIntegrationMock.getLegalIds(MUNICIPALITY_ID, List.of(partyId1, partyId2))).thenReturn(Map.of(
			partyId1, "legalId1",
			partyId2, "legalId2"));

		final var result = historyService.getMessageDetails(MUNICIPALITY_ID, userId, messageId);

		assertThat(result).isNotNull().satisfies(messageDetails -> {
			assertThat(messageDetails.getRecipients()).hasSize(3);
			// Verify recipient with partyId1 has legalId
			assertThat(messageDetails.getRecipients().getFirst().getPartyId()).isEqualTo(partyId1);
			assertThat(messageDetails.getRecipients().getFirst().getLegalId()).isEqualTo("legalId1");
			// Verify recipient without partyId has no legalId
			assertThat(messageDetails.getRecipients().get(1).getPartyId()).isNull();
			assertThat(messageDetails.getRecipients().get(1).getLegalId()).isNull();
			// Verify recipient with partyId2 has legalId
			assertThat(messageDetails.getRecipients().getLast().getPartyId()).isEqualTo(partyId2);
			assertThat(messageDetails.getRecipients().getLast().getLegalId()).isEqualTo("legalId2");
		});

		verify(messageRepositoryMock).findByMunicipalityIdAndIdAndUserUsernameIgnoreCase(MUNICIPALITY_ID, messageId, userId);
		verify(partyIntegrationMock).getLegalIds(MUNICIPALITY_ID, List.of(partyId1, partyId2));
		verify(historyMapperMock).toMessageDetails(message);
		verify(historyMapperMock).toAttachmentList(message.getAttachments());
		verify(historyMapperMock).toRecipientList(message.getRecipients());
		verify(historyMapperMock).toRecipient(recipientWithPartyId1);
		verify(historyMapperMock).toRecipient(recipientWithoutPartyId);
		verify(historyMapperMock).toRecipient(recipientWithPartyId2);
	}

	@Test
	void getMessageDetails_withNoRecipientsHavingPartyIds() {
		final var messageId = "messageId";
		final var userId = "userId";
		// All recipients are SMS (no partyIds)
		final var smsRecipient1 = new RecipientEntity().withPartyId(null).withPhoneNumber("+46701234567");
		final var smsRecipient2 = new RecipientEntity().withPartyId(null).withPhoneNumber("+46709876543");
		final var message = MessageEntity.create()
			.withSubject("subject")
			.withCreated(OffsetDateTime.now())
			.withAttachments(List.of())
			.withRecipients(List.of(smsRecipient1, smsRecipient2));

		when(messageRepositoryMock.findByMunicipalityIdAndIdAndUserUsernameIgnoreCase(MUNICIPALITY_ID, messageId, userId)).thenReturn(Optional.of(message));
		// Empty list should be sent to party integration when no partyIds
		when(partyIntegrationMock.getLegalIds(MUNICIPALITY_ID, emptyList())).thenReturn(Map.of());

		final var result = historyService.getMessageDetails(MUNICIPALITY_ID, userId, messageId);

		assertThat(result).isNotNull().satisfies(messageDetails -> {
			assertThat(messageDetails.getRecipients()).hasSize(2);
			assertThat(messageDetails.getRecipients()).allSatisfy(recipient -> {
				assertThat(recipient.getPartyId()).isNull();
				assertThat(recipient.getLegalId()).isNull();
			});
		});

		verify(messageRepositoryMock).findByMunicipalityIdAndIdAndUserUsernameIgnoreCase(MUNICIPALITY_ID, messageId, userId);
		verify(partyIntegrationMock).getLegalIds(MUNICIPALITY_ID, emptyList());
		verify(historyMapperMock).toMessageDetails(message);
		verify(historyMapperMock).toAttachmentList(message.getAttachments());
		verify(historyMapperMock).toRecipientList(message.getRecipients());
		verify(historyMapperMock).toRecipient(smsRecipient1);
		verify(historyMapperMock).toRecipient(smsRecipient2);
	}

	@Test
	void getMessageDetails_withPartyIdNotFoundInLegalIdMap() {
		final var messageId = "messageId";
		final var userId = "userId";
		final var partyId1 = "partyId1";
		final var partyId2 = "partyId2";
		final var recipient1 = new RecipientEntity().withPartyId(partyId1);
		final var recipient2 = new RecipientEntity().withPartyId(partyId2);
		final var message = MessageEntity.create()
			.withSubject("subject")
			.withCreated(OffsetDateTime.now())
			.withAttachments(List.of())
			.withRecipients(List.of(recipient1, recipient2));

		when(messageRepositoryMock.findByMunicipalityIdAndIdAndUserUsernameIgnoreCase(MUNICIPALITY_ID, messageId, userId)).thenReturn(Optional.of(message));
		// Party integration returns only one of the partyIds
		when(partyIntegrationMock.getLegalIds(MUNICIPALITY_ID, List.of(partyId1, partyId2))).thenReturn(Map.of(partyId1, "legalId1"));

		final var result = historyService.getMessageDetails(MUNICIPALITY_ID, userId, messageId);

		assertThat(result).isNotNull().satisfies(messageDetails -> {
			assertThat(messageDetails.getRecipients()).hasSize(2);
			// First recipient should have legalId
			assertThat(messageDetails.getRecipients().getFirst().getPartyId()).isEqualTo(partyId1);
			assertThat(messageDetails.getRecipients().getFirst().getLegalId()).isEqualTo("legalId1");
			// Second recipient should have null legalId (not found in map)
			assertThat(messageDetails.getRecipients().getLast().getPartyId()).isEqualTo(partyId2);
			assertThat(messageDetails.getRecipients().getLast().getLegalId()).isNull();
		});

		verify(messageRepositoryMock).findByMunicipalityIdAndIdAndUserUsernameIgnoreCase(MUNICIPALITY_ID, messageId, userId);
		verify(partyIntegrationMock).getLegalIds(MUNICIPALITY_ID, List.of(partyId1, partyId2));
		verify(historyMapperMock).toMessageDetails(message);
		verify(historyMapperMock).toAttachmentList(message.getAttachments());
		verify(historyMapperMock).toRecipientList(message.getRecipients());
		verify(historyMapperMock).toRecipient(recipient1);
		verify(historyMapperMock).toRecipient(recipient2);
	}

	@Test
	void getSigningInformation_notFound() {
		final var messageId = "messageId";
		when(messageRepositoryMock.findByIdAndMessageType(messageId, DIGITAL_REGISTERED_LETTER)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> historyService.getSigningInformation(MUNICIPALITY_ID, messageId))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Not Found: No digital registered letter found for id '%s'".formatted(messageId));

		verify(messageRepositoryMock).findByIdAndMessageType(messageId, DIGITAL_REGISTERED_LETTER);
		verifyNoInteractions(digitalRegisteredLetterIntegrationMock);
		verifyNoMoreInteractions(messageRepositoryMock);
	}

	@Test
	void getSigningInformation() {
		final var messageId = "messageId";
		final var externalId = "123asd";
		final var recipientEntity = new RecipientEntity().withExternalId(externalId);
		final var messageEntity = new MessageEntity()
			.withRecipients(List.of(recipientEntity));
		final var signingInformation = new SigningInformation().withContentKey("someValue");

		when(messageRepositoryMock.findByIdAndMessageType(messageId, DIGITAL_REGISTERED_LETTER)).thenReturn(Optional.of(messageEntity));
		when(digitalRegisteredLetterIntegrationMock.getSigningInformation(MUNICIPALITY_ID, externalId)).thenReturn(signingInformation);

		final var result = historyService.getSigningInformation(MUNICIPALITY_ID, messageId);

		assertThat(result).isNotNull().isEqualTo(signingInformation);
		verify(messageRepositoryMock).findByIdAndMessageType(messageId, DIGITAL_REGISTERED_LETTER);
		verify(digitalRegisteredLetterIntegrationMock).getSigningInformation(MUNICIPALITY_ID, externalId);
		verifyNoMoreInteractions(messageRepositoryMock, digitalRegisteredLetterIntegrationMock);
	}

	@Test
	void getLetterReceipt_notFound() {
		final var messageId = "messageId";
		when(messageRepositoryMock.findByIdAndMessageType(messageId, DIGITAL_REGISTERED_LETTER)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> historyService.getLetterReceipt(MUNICIPALITY_ID, messageId))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Not Found: No digital registered letter found for id '%s'".formatted(messageId));

		verify(messageRepositoryMock).findByIdAndMessageType(messageId, DIGITAL_REGISTERED_LETTER);
		verifyNoInteractions(digitalRegisteredLetterIntegrationMock);
		verifyNoMoreInteractions(messageRepositoryMock);
	}

	@Test
	void getLetterReceipt() {
		final var messageId = "messageId";
		final var externalId = "123asd";
		final var recipientEntity = new RecipientEntity().withExternalId(externalId);
		final var messageEntity = new MessageEntity()
			.withRecipients(List.of(recipientEntity));
		final var mockResponseEntity = ok()
			.header("Content-Type", "application/pdf")
			.body((StreamingResponseBody) outputStream -> outputStream.write("test data".getBytes()));

		when(messageRepositoryMock.findByIdAndMessageType(messageId, DIGITAL_REGISTERED_LETTER)).thenReturn(Optional.of(messageEntity));
		when(digitalRegisteredLetterIntegrationMock.getLetterReceipt(MUNICIPALITY_ID, externalId)).thenReturn(mockResponseEntity);

		final var result = historyService.getLetterReceipt(MUNICIPALITY_ID, messageId);

		assertThat(result).isNotNull().isEqualTo(mockResponseEntity);
		verify(messageRepositoryMock).findByIdAndMessageType(messageId, DIGITAL_REGISTERED_LETTER);
		verify(digitalRegisteredLetterIntegrationMock).getLetterReceipt(MUNICIPALITY_ID, externalId);
		verifyNoMoreInteractions(messageRepositoryMock, digitalRegisteredLetterIntegrationMock);
	}

}
