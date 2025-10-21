package se.sundsvall.postportalservice.service;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.postportalservice.TestDataFactory.MUNICIPALITY_ID;
import static se.sundsvall.postportalservice.integration.db.converter.MessageType.DIGITAL_REGISTERED_LETTER;

import generated.se.sundsvall.digitalregisteredletter.LetterStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.zalando.problem.Problem;
import se.sundsvall.postportalservice.api.model.SigningInformation;
import se.sundsvall.postportalservice.api.model.SigningStatus;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.db.converter.MessageType;
import se.sundsvall.postportalservice.integration.db.dao.MessageRepository;
import se.sundsvall.postportalservice.integration.digitalregisteredletter.DigitalRegisteredLetterIntegration;
import se.sundsvall.postportalservice.service.mapper.HistoryMapper;

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

	@InjectMocks
	private HistoryService historyService;

	@AfterEach
	void ensureNoUnexpectedMockInteractions() {
		verifyNoMoreInteractions(messageRepositoryMock, pageMock, historyMapperMock, digitalRegisteredLetterIntegrationMock);
	}

	@ParameterizedTest
	@EnumSource(value = MessageType.class, mode = EXCLUDE, names = "DIGITAL_REGISTERED_LETTER")
	void getUserMessages_noDigitalRegisteredLettersCommunication(MessageType messageType) {
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
		verify(digitalRegisteredLetterIntegrationMock).getLetterStatuses(MUNICIPALITY_ID, emptyList());
		verify(historyMapperMock).toMessageList(messageEntities);
		verify(historyMapperMock).toMessage(messageEntity);
		verify(pageMock).getContent();
	}

	@Test
	void getUserMessages_digitalRegisteredLettersCommunicationWithMatchingLetterStatus() {
		final var username = "username";
		final var messageId = "messageId";
		final var letterState = "letterState";
		final var signingProcessState = "signingProcessState";
		final var messageEntity = MessageEntity.create()
			.withCreated(OffsetDateTime.now())
			.withSubject("subject")
			.withMessageType(MessageType.DIGITAL_REGISTERED_LETTER)
			.withId(messageId);
		final var messageEntities = List.of(messageEntity);
		final var letterStatus = new LetterStatus()
			.letterId(messageId)
			.status(letterState);
		final var signingStatus = SigningStatus.create()
			.withLetterState(letterState)
			.withSigningProcessState(signingProcessState);

		when(messageRepositoryMock.findAllByMunicipalityIdAndUserUsernameIgnoreCase(eq(MUNICIPALITY_ID), eq(username), any(Pageable.class))).thenReturn(pageMock);
		when(digitalRegisteredLetterIntegrationMock.getLetterStatuses(MUNICIPALITY_ID, List.of(messageId))).thenReturn(List.of(letterStatus));
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
		verify(digitalRegisteredLetterIntegrationMock).getLetterStatuses(MUNICIPALITY_ID, List.of(messageId));
		verify(historyMapperMock).toMessageList(messageEntities);
		verify(historyMapperMock).toMessage(messageEntity);
		verify(historyMapperMock).toSigningStatus(letterStatus);
		verify(pageMock).getContent();
	}

	@Test
	void getUserMessages_digitalRegisteredLettersCommunicationWithNonMatchingStatus() {
		final var username = "username";
		final var messageId = "messageId";
		final var status = "COMPLETED";
		final var messageEntity = MessageEntity.create()
			.withCreated(OffsetDateTime.now())
			.withSubject("subject")
			.withMessageType(MessageType.DIGITAL_REGISTERED_LETTER)
			.withId(messageId);
		final var messageEntities = List.of(messageEntity);
		final var letterStatus = new LetterStatus()
			.letterId("otherMessageId")
			.status(status);

		when(messageRepositoryMock.findAllByMunicipalityIdAndUserUsernameIgnoreCase(eq(MUNICIPALITY_ID), eq(username), any(Pageable.class))).thenReturn(pageMock);
		when(digitalRegisteredLetterIntegrationMock.getLetterStatuses(MUNICIPALITY_ID, List.of(messageId))).thenReturn(List.of(letterStatus));
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
		verify(digitalRegisteredLetterIntegrationMock).getLetterStatuses(MUNICIPALITY_ID, List.of(messageId));
		verify(historyMapperMock).toMessageList(messageEntities);
		verify(historyMapperMock).toMessage(messageEntity);
		verify(pageMock).getContent();
	}

	@Test
	void getMessageDetails() {
		final var messageId = "messageId";
		final var userId = "userId";
		final var message = MessageEntity.create()
			.withSubject("subject")
			.withCreated(OffsetDateTime.now())
			.withAttachments(List.of())
			.withRecipients(List.of());

		when(messageRepositoryMock.findByMunicipalityIdAndIdAndUserUsernameIgnoreCase(MUNICIPALITY_ID, messageId, userId)).thenReturn(Optional.of(message));

		final var result = historyService.getMessageDetails(MUNICIPALITY_ID, userId, messageId);

		assertThat(result).isNotNull().satisfies(messageDetails -> {
			assertThat(messageDetails.getSubject()).isEqualTo(message.getSubject());
			assertThat(messageDetails.getSentAt()).isEqualTo(message.getCreated().toLocalDateTime());
			assertThat(messageDetails.getAttachments()).isEmpty();
			assertThat(messageDetails.getRecipients()).isEmpty();
		});
		verify(messageRepositoryMock).findByMunicipalityIdAndIdAndUserUsernameIgnoreCase(MUNICIPALITY_ID, messageId, userId);
		verify(historyMapperMock).toMessageDetails(message);
		verify(historyMapperMock).toAttachmentList(message.getAttachments());
		verify(historyMapperMock).toRecipientList(message.getRecipients());
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

}
