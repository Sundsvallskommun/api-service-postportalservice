package se.sundsvall.postportalservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.BAD_GATEWAY;
import static se.sundsvall.postportalservice.Constants.FAILED;
import static se.sundsvall.postportalservice.Constants.SENT;
import static se.sundsvall.postportalservice.TestDataFactory.MUNICIPALITY_ID;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.CONTACT_INFORMATION_EMAIL;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.CONTACT_INFORMATION_PHONE_NUMBER;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.CONTACT_INFORMATION_URL;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.DEPARTMENT_ID;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.DEPARTMENT_NAME;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.FOLDER_NAME;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.ORGANIZATION_NUMBER;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.SMS_SENDER;
import static se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration.SUPPORT_TEXT;

import generated.se.sundsvall.messaging.DeliveryResult;
import generated.se.sundsvall.messaging.MessageBatchResult;
import generated.se.sundsvall.messaging.MessageResult;
import generated.se.sundsvall.messaging.MessageStatus;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import org.zalando.problem.Problem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.postportalservice.TestDataFactory;
import se.sundsvall.postportalservice.api.model.Address;
import se.sundsvall.postportalservice.api.model.Recipient;
import se.sundsvall.postportalservice.api.model.SmsRecipient;
import se.sundsvall.postportalservice.integration.db.AttachmentEntity;
import se.sundsvall.postportalservice.integration.db.DepartmentEntity;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.db.UserEntity;
import se.sundsvall.postportalservice.integration.db.converter.MessageType;
import se.sundsvall.postportalservice.integration.db.dao.DepartmentRepository;
import se.sundsvall.postportalservice.integration.db.dao.MessageRepository;
import se.sundsvall.postportalservice.integration.db.dao.RecipientRepository;
import se.sundsvall.postportalservice.integration.db.dao.UserRepository;
import se.sundsvall.postportalservice.integration.digitalregisteredletter.DigitalRegisteredLetterIntegration;
import se.sundsvall.postportalservice.integration.messaging.MessagingIntegration;
import se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration;
import se.sundsvall.postportalservice.service.mapper.AttachmentMapper;
import se.sundsvall.postportalservice.service.mapper.EntityMapper;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

	private static final String USERNAME = "username";
	private static final Map<String, String> SETTINGS_MAP = Map.of(
		DEPARTMENT_ID, "departmentId",
		DEPARTMENT_NAME, "departmentName",
		ORGANIZATION_NUMBER, "123456789",
		FOLDER_NAME, "folderName",
		SMS_SENDER, "smsSender",
		SUPPORT_TEXT, "supportText",
		CONTACT_INFORMATION_URL, "contactInformationUrl",
		CONTACT_INFORMATION_PHONE_NUMBER, "contactInformationPhoneNumber",
		CONTACT_INFORMATION_EMAIL, "contactInformationEmail");

	@Mock
	private MessagingIntegration messagingIntegrationMock;

	@Mock
	private DepartmentRepository departmentRepositoryMock;

	@Mock
	private AttachmentMapper attachmentMapperMock;

	@Mock(answer = Answers.CALLS_REAL_METHODS)
	private EntityMapper entityMapperMock;

	@Mock
	private UserRepository userRepositoryMock;

	@Mock
	private MessageRepository messageRepositoryMock;

	@Mock
	private RecipientRepository recipientRepositoryMock;

	@Mock
	private MessagingSettingsIntegration messagingSettingsIntegrationMock;

	@Mock
	private DigitalRegisteredLetterIntegration digitalRegisteredLetterIntegrationMock;

	@Captor
	private ArgumentCaptor<MessageEntity> messageEntityCaptor;

	@InjectMocks
	private MessageService messageService;

	@BeforeEach
	void setup() {
		final var identifier = Identifier.create()
			.withType(Identifier.Type.AD_ACCOUNT)
			.withValue(USERNAME)
			.withTypeString("AD_ACCOUNT");
		Identifier.set(identifier);
	}

	@AfterEach
	void tearDown() {
		Identifier.remove();
		verifyNoMoreInteractions(attachmentMapperMock, entityMapperMock,
			messagingIntegrationMock,
			departmentRepositoryMock, userRepositoryMock,
			messageRepositoryMock, recipientRepositoryMock, digitalRegisteredLetterIntegrationMock);
	}

	/**
	 * Messaging settings integration throws an exception, we expect a Problem (502 Bad Gateway) to be thrown and the
	 * process to stop.
	 */
	@Test
	void processDigitalRegisteredLetterRequest_messagingSettingsThrows() {
		final var request = TestDataFactory.createValidDigitalRegisteredLetterRequest();
		final var multipartFile = Mockito.mock(MultipartFile.class);
		final var multipartFiles = List.of(multipartFile);
		Identifier.set(new Identifier().withValue("test01user"));

		when(messagingSettingsIntegrationMock.getMessagingSettingsForUser(MUNICIPALITY_ID))
			.thenThrow(Problem.valueOf(BAD_GATEWAY, "No messaging settings found for user '%s' in municipalityId '%s'".formatted("test01user", MUNICIPALITY_ID)));

		assertThatThrownBy(() -> messageService.processDigitalRegisteredLetterRequest(MUNICIPALITY_ID, request, multipartFiles))
			.isInstanceOf(Problem.class)
			.hasMessage("Bad Gateway: No messaging settings found for user '%s' in municipalityId '%s'".formatted("test01user", MUNICIPALITY_ID));

		verify(messagingSettingsIntegrationMock).getMessagingSettingsForUser(MUNICIPALITY_ID);
		verifyNoInteractions(messagingIntegrationMock, departmentRepositoryMock, userRepositoryMock, messageRepositoryMock);
	}

	/**
	 * If the DigitalRegisteredLetter integration throws an exception. We expect the exception to be swallowed and the
	 * recipient to be marked as FAILED with the exception message as status detail. The process should continue and the
	 * message entity should
	 * be saved with the recipient marked as FAILED.
	 */
	@Test
	void processDigitalRegisteredLetterRequest_digitalRegisteredLetterThrows() {
		final var request = TestDataFactory.createValidDigitalRegisteredLetterRequest();
		final var multipartFile = Mockito.mock(MultipartFile.class);
		final var multipartFiles = List.of(multipartFile);

		final var attachmentEntities = List.of(new AttachmentEntity(), new AttachmentEntity());

		when(messagingSettingsIntegrationMock.getMessagingSettingsForUser(MUNICIPALITY_ID)).thenReturn(SETTINGS_MAP);
		when(messageRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0, MessageEntity.class).withId("messageId"));
		doAnswer(inv -> {
			final var recipientEntity = inv.getArgument(1, RecipientEntity.class);
			recipientEntity.setStatus(FAILED);
			recipientEntity.setStatusDetail("Something when wrong");
			return null;
		}).when(digitalRegisteredLetterIntegrationMock).sendLetter(any(MessageEntity.class), any(RecipientEntity.class));
		when(userRepositoryMock.findByUsernameIgnoreCase(USERNAME)).thenReturn(Optional.empty());
		when(departmentRepositoryMock.findByOrganizationId("departmentId")).thenReturn(Optional.empty());
		when(attachmentMapperMock.toAttachmentEntities(multipartFiles)).thenReturn(attachmentEntities);

		final var result = messageService.processDigitalRegisteredLetterRequest(MUNICIPALITY_ID, request, multipartFiles);

		assertThat(result).isNotNull().isEqualTo("messageId");

		verify(userRepositoryMock).findByUsernameIgnoreCase(USERNAME);
		verify(departmentRepositoryMock).findByOrganizationId("departmentId");
		verify(digitalRegisteredLetterIntegrationMock).sendLetter(any(), any());
		verify(messageRepositoryMock).save(messageEntityCaptor.capture());
		final var messageEntity = messageEntityCaptor.getValue();
		assertThat(messageEntity).isNotNull();
		assertThat(messageEntity.getId()).isEqualTo("messageId");
		assertThat(messageEntity.getAttachments()).isEqualTo(attachmentEntities);
		assertThat(messageEntity.getDepartment()).isNotNull().isInstanceOf(DepartmentEntity.class).satisfies(departmentEntity -> {
			// Asserts that a new department was created with the correct values since none existed previously
			assertThat(departmentEntity.getId()).isNull();
			assertThat(departmentEntity.getName()).isEqualTo("departmentName");
			assertThat(departmentEntity.getOrganizationId()).isEqualTo("departmentId");
		});
		assertThat(messageEntity.getUser()).isNotNull().isInstanceOf(UserEntity.class).satisfies(userEntity -> {
			// Asserts that a new user was created with the correct values since none existed previously
			assertThat(userEntity.getId()).isNull();
			assertThat(userEntity.getUsername()).isEqualTo(USERNAME);
		});
		assertThat(messageEntity.getRecipients().getFirst()).isNotNull().isInstanceOf(RecipientEntity.class).satisfies(recipientEntity -> {
			assertThat(recipientEntity.getPartyId()).isEqualTo(request.getPartyId());
			assertThat(recipientEntity.getMessageType()).isEqualTo(MessageType.DIGITAL_REGISTERED_LETTER);
			assertThat(recipientEntity.getStatus()).isEqualTo(FAILED);
			assertThat(recipientEntity.getStatusDetail()).isEqualTo("Something when wrong");
			assertThat(recipientEntity.getExternalId()).isNull();
		});

		verifyNoInteractions(messagingIntegrationMock);
	}

	/**
	 * Happy case where everything works as expected.
	 */
	@Test
	void processDigitalRegisteredLetterRequest_happyCase() {
		final var request = TestDataFactory.createValidDigitalRegisteredLetterRequest();
		final var multipartFile = Mockito.mock(MultipartFile.class);
		final var multipartFileList = List.of(multipartFile);
		final var attachmentEntities = List.of(new AttachmentEntity(), new AttachmentEntity());
		final var userEntity = new UserEntity().withUsername(USERNAME).withId("userId");
		final var departmentEntity = new DepartmentEntity().withName("departmentName").withOrganizationId("departmentId").withId("departmentId");

		when(messagingSettingsIntegrationMock.getMessagingSettingsForUser(MUNICIPALITY_ID)).thenReturn(SETTINGS_MAP);

		when(messageRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0, MessageEntity.class).withId("messageId"));
		doAnswer(inv -> {
			final var recipientEntity = inv.getArgument(1, RecipientEntity.class);
			recipientEntity.setStatus(SENT);
			recipientEntity.setExternalId("229a3e3e-17ae-423a-9a14-671b5b1bbd17");
			return null;
		}).when(digitalRegisteredLetterIntegrationMock).sendLetter(any(MessageEntity.class), any(RecipientEntity.class));
		when(userRepositoryMock.findByUsernameIgnoreCase(USERNAME)).thenReturn(Optional.of(userEntity));
		when(departmentRepositoryMock.findByOrganizationId("departmentId")).thenReturn(Optional.of(departmentEntity));
		when(attachmentMapperMock.toAttachmentEntities(multipartFileList)).thenReturn(attachmentEntities);

		final var result = messageService.processDigitalRegisteredLetterRequest(MUNICIPALITY_ID, request, multipartFileList);

		assertThat(result).isNotNull().isEqualTo("messageId");

		verify(userRepositoryMock).findByUsernameIgnoreCase(USERNAME);
		verify(departmentRepositoryMock).findByOrganizationId("departmentId");
		verify(digitalRegisteredLetterIntegrationMock).sendLetter(any(), any());
		verify(messageRepositoryMock).save(messageEntityCaptor.capture());
		final var messageEntity = messageEntityCaptor.getValue();
		assertThat(messageEntity).isNotNull();
		assertThat(messageEntity.getId()).isEqualTo("messageId");
		assertThat(messageEntity.getAttachments()).isEqualTo(attachmentEntities);
		assertThat(messageEntity.getDepartment()).isNotNull().isInstanceOf(DepartmentEntity.class).satisfies(entity -> {
			// Asserts that a new department was created with the correct values since none existed previously
			assertThat(departmentEntity.getId()).isEqualTo("departmentId");
			assertThat(departmentEntity.getName()).isEqualTo("departmentName");
			assertThat(departmentEntity.getOrganizationId()).isEqualTo("departmentId");
		});
		assertThat(messageEntity.getUser()).isNotNull().isInstanceOf(UserEntity.class).satisfies(entity -> {
			// Asserts that a new user was created with the correct values since none existed previously
			assertThat(userEntity.getId()).isEqualTo("userId");
			assertThat(userEntity.getUsername()).isEqualTo(USERNAME);
		});
		assertThat(messageEntity.getRecipients().getFirst()).isNotNull().isInstanceOf(RecipientEntity.class).satisfies(recipientEntity -> {
			assertThat(recipientEntity.getPartyId()).isEqualTo(request.getPartyId());
			assertThat(recipientEntity.getMessageType()).isEqualTo(MessageType.DIGITAL_REGISTERED_LETTER);
			assertThat(recipientEntity.getStatus()).isEqualTo(SENT);
			assertThat(recipientEntity.getExternalId()).isEqualTo("229a3e3e-17ae-423a-9a14-671b5b1bbd17");
			assertThat(recipientEntity.getStatusDetail()).isNull();
		});

		verifyNoInteractions(messagingIntegrationMock);
	}

	@Test
	void processLetterRequest() {
		final var spy = Mockito.spy(messageService);
		final var multipartFile = Mockito.mock(MultipartFile.class);
		final var multipartFileList = List.of(multipartFile);
		final var letterRequest = TestDataFactory.createValidLetterRequest();
		final var userEntity = new UserEntity().withUsername("username");
		final var departmentEntity = new DepartmentEntity().withName("departmentName").withOrganizationId("departmentId");
		final var attachmentEntity = new AttachmentEntity().withFileName("filename").withContentType("contentType").withContent(null);
		final var messageId = "adc63e5c-b92f-4c75-b14f-819473cef5b6";

		final var identifier = Identifier.create()
			.withType(Identifier.Type.AD_ACCOUNT)
			.withValue("username")
			.withTypeString("AD_ACCOUNT");
		Identifier.set(identifier);

		when(messagingSettingsIntegrationMock.getMessagingSettingsForUser(MUNICIPALITY_ID)).thenReturn(SETTINGS_MAP);
		when(attachmentMapperMock.toAttachmentEntities(multipartFileList)).thenReturn(List.of(attachmentEntity));
		when(userRepositoryMock.findByUsernameIgnoreCase(Identifier.get().getValue())).thenReturn(Optional.of(userEntity));
		when(departmentRepositoryMock.findByOrganizationId(SETTINGS_MAP.get(DEPARTMENT_ID))).thenReturn(Optional.of(departmentEntity));

		doReturn(new CompletableFuture<>()).when(spy).processRecipients(any());
		when(messageRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0, MessageEntity.class).withId(messageId));

		final var result = spy.processLetterRequest(MUNICIPALITY_ID, letterRequest, multipartFileList);

		assertThat(result).isEqualTo(messageId);
		verify(attachmentMapperMock).toAttachmentEntities(multipartFileList);
		verify(messagingSettingsIntegrationMock).getMessagingSettingsForUser(MUNICIPALITY_ID);
		verify(userRepositoryMock).findByUsernameIgnoreCase(Identifier.get().getValue());
		verify(departmentRepositoryMock).findByOrganizationId(SETTINGS_MAP.get(DEPARTMENT_ID));
		verify(entityMapperMock).toRecipientEntity(any(Address.class));
		verify(entityMapperMock).toRecipientEntity(any(Recipient.class));
		verify(spy).processRecipients(any());
		verify(messageRepositoryMock).save(any());
	}

	@Test
	void processSmsRequest() {
		final var spy = Mockito.spy(messageService);
		final var smsRequest = TestDataFactory.createValidSmsRequest();
		final var userEntity = new UserEntity().withUsername("username");
		final var departmentEntity = new DepartmentEntity().withName("departmentName").withOrganizationId("departmentId");
		final var messageId = "adc63e5c-b92f-4c75-b14f-819473cef5b6";

		when(messagingSettingsIntegrationMock.getMessagingSettingsForUser(MUNICIPALITY_ID)).thenReturn(SETTINGS_MAP);
		when(userRepositoryMock.findByUsernameIgnoreCase(Identifier.get().getValue())).thenReturn(Optional.of(userEntity));
		when(departmentRepositoryMock.findByOrganizationId(SETTINGS_MAP.get(DEPARTMENT_ID))).thenReturn(Optional.of(departmentEntity));
		doReturn(new CompletableFuture<>()).when(spy).processRecipients(any());
		when(messageRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0, MessageEntity.class).withId(messageId));

		final var result = spy.processSmsRequest(MUNICIPALITY_ID, smsRequest);

		assertThat(result).isEqualTo(messageId);
		verify(messagingSettingsIntegrationMock).getMessagingSettingsForUser(MUNICIPALITY_ID);
		verify(userRepositoryMock).findByUsernameIgnoreCase(Identifier.get().getValue());
		verify(departmentRepositoryMock).findByOrganizationId(SETTINGS_MAP.get(DEPARTMENT_ID));
		verify(entityMapperMock).toRecipientEntity(any(SmsRecipient.class));
		verify(spy).processRecipients(any());
		verify(messageRepositoryMock).save(any());
	}

	@Test
	void processRecipients() {
		final var spy = Mockito.spy(messageService);
		final var recipient1 = new RecipientEntity().withFirstName("john");
		final var recipient2 = new RecipientEntity().withFirstName("sarah");

		final var messageEntity = MessageEntity.create()
			.withRecipients(List.of(recipient1, recipient2));

		final var future1 = new CompletableFuture<Void>();
		final var future2 = new CompletableFuture<Void>();
		doReturn(future1).when(spy).sendMessageToRecipient(messageEntity, recipient1);
		doReturn(future2).when(spy).sendMessageToRecipient(messageEntity, recipient2);

		final var completableFuture = spy.processRecipients(messageEntity);
		future1.complete(null);
		future2.complete(null);
		completableFuture.join();

		verify(spy).sendMessageToRecipient(messageEntity, recipient1);
		verify(spy).sendMessageToRecipient(messageEntity, recipient2);
		verify(messageRepositoryMock).save(messageEntity);
	}

	@Test
	void processRecipients_future_completes_exceptionally() {
		final var spy = Mockito.spy(messageService);
		final var recipient1 = new RecipientEntity().withFirstName("john");
		final var recipient2 = new RecipientEntity().withFirstName("sarah");

		final var messageEntity = MessageEntity.create()
			.withRecipients(List.of(recipient1, recipient2));

		final var future1 = new CompletableFuture<Void>();
		final var future2 = new CompletableFuture<Void>();
		doReturn(future1).when(spy).sendMessageToRecipient(messageEntity, recipient1);
		doReturn(future2).when(spy).sendMessageToRecipient(messageEntity, recipient2);

		final var completableFuture = spy.processRecipients(messageEntity);

		future1.completeExceptionally(new RuntimeException("Simulated exception"));
		future2.complete(null);

		completableFuture.join();

		verify(spy, times(2)).sendMessageToRecipient(any(), any());
		verify(messageRepositoryMock).save(messageEntity);
	}

	@Test
	void processRecipients_future_delayed() {
		final var spy = Mockito.spy(messageService);
		final var recipient1 = new RecipientEntity().withFirstName("john");
		final var recipient2 = new RecipientEntity().withFirstName("sarah");
		final var messageEntity = MessageEntity.create().withRecipients(List.of(recipient1, recipient2));

		final var future1 = new CompletableFuture<Void>();
		final var future2 = new CompletableFuture<Void>();
		doReturn(future1).when(spy).sendMessageToRecipient(messageEntity, recipient1);
		doReturn(future2).when(spy).sendMessageToRecipient(messageEntity, recipient2);

		final var completableFuture = spy.processRecipients(messageEntity);

		future1.complete(null);

		try {
			Thread.sleep(3000);
			future2.complete(null);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		completableFuture.join();

		verify(messageRepositoryMock, times(1)).save(messageEntity);
	}

	@Test
	void sendMessageToRecipient_SMS() {
		final var spy = Mockito.spy(messageService);

		final var recipient1 = new RecipientEntity().withFirstName("john").withMessageType(MessageType.SMS);
		final var messageEntity = MessageEntity.create().withRecipients(List.of(recipient1));
		doReturn(new CompletableFuture<>()).when(spy).sendSmsToRecipient(messageEntity, recipient1);

		spy.sendMessageToRecipient(messageEntity, recipient1);

		verify(spy).sendSmsToRecipient(messageEntity, recipient1);
	}

	@Test
	void sendMessageToRecipient_digitalMail() {
		final var spy = Mockito.spy(messageService);

		final var recipient1 = new RecipientEntity().withFirstName("john").withMessageType(MessageType.DIGITAL_MAIL);
		final var messageEntity = MessageEntity.create().withRecipients(List.of(recipient1));
		doReturn(new CompletableFuture<>()).when(spy).sendDigitalMailToRecipient(messageEntity, recipient1);

		spy.sendMessageToRecipient(messageEntity, recipient1);

		verify(spy).sendDigitalMailToRecipient(messageEntity, recipient1);
	}

	@Test
	void sendMessageToRecipient_snailMail() {
		final var spy = Mockito.spy(messageService);

		final var recipient1 = new RecipientEntity().withFirstName("john").withMessageType(MessageType.SNAIL_MAIL);
		final var messageEntity = MessageEntity.create().withRecipients(List.of(recipient1));
		doReturn(new CompletableFuture<>()).when(spy).sendSnailMailToRecipient(messageEntity, recipient1);

		spy.sendMessageToRecipient(messageEntity, recipient1);

		verify(spy).sendSnailMailToRecipient(messageEntity, recipient1);
	}

	@Test
	void sendMessageToRecipient_unsupportedMessageType() {
		final var recipient1 = new RecipientEntity().withFirstName("john").withMessageType(MessageType.LETTER);
		final var messageEntity = MessageEntity.create().withRecipients(List.of(recipient1));

		messageService.sendMessageToRecipient(messageEntity, recipient1);

		assertThat(recipient1.getStatus()).isEqualTo(FAILED);
		assertThat(recipient1.getStatusDetail()).isEqualTo("Unsupported message type: LETTER");
	}

	@Test
	void processSmsRequestToRecipient() {
		final var spy = Mockito.spy(messageService);
		final var recipient1 = new RecipientEntity().withFirstName("john");
		final var messageEntity = MessageEntity.create().withRecipients(List.of(recipient1));
		final var uuid = UUID.randomUUID();
		final var messageResult = new MessageResult()
			.messageId(uuid)
			.deliveries(List.of(new DeliveryResult()
				.status(MessageStatus.SENT)));

		when(messagingIntegrationMock.sendSms(messageEntity, recipient1)).thenReturn(messageResult);
		doCallRealMethod().when(spy).updateRecipient(messageResult, recipient1);

		final var completableFuture = spy.sendSmsToRecipient(messageEntity, recipient1);

		completableFuture.join();
		assertThat(recipient1.getStatus()).isEqualTo(MessageStatus.SENT.toString());
		assertThat(recipient1.getExternalId()).isEqualTo(uuid.toString());
		verify(messagingIntegrationMock).sendSms(messageEntity, recipient1);
		verify(recipientRepositoryMock).save(recipient1);
	}

	@Test
	void processSmsRequestToRecipient_exception() {
		final var spy = Mockito.spy(messageService);
		final var recipient1 = new RecipientEntity().withFirstName("john");
		final var messageEntity = MessageEntity.create().withRecipients(List.of(recipient1));

		when(messagingIntegrationMock.sendSms(messageEntity, recipient1)).thenThrow(new RuntimeException("Simulated exception"));

		final var completableFuture = spy.sendSmsToRecipient(messageEntity, recipient1);

		completableFuture.join();
		assertThat(recipient1.getStatus()).isEqualTo(MessageStatus.FAILED.toString());
		assertThat(recipient1.getStatusDetail()).isEqualTo("java.lang.RuntimeException: Simulated exception");
		verify(messagingIntegrationMock).sendSms(messageEntity, recipient1);
		verify(recipientRepositoryMock).save(recipient1);
	}

	@Test
	void sendDigitalMailToRecipient_success() {
		final var spy = Mockito.spy(messageService);
		final var recipient1 = new RecipientEntity().withFirstName("john");
		final var messageEntity = MessageEntity.create().withRecipients(List.of(recipient1));
		final var uuid = UUID.randomUUID();
		final var messageResult = new MessageResult()
			.messageId(uuid)
			.deliveries(List.of(new DeliveryResult()
				.status(MessageStatus.SENT)));
		final var messageBatchResult = new MessageBatchResult()
			.messages(List.of(messageResult));

		when(messagingIntegrationMock.sendDigitalMail(messageEntity, recipient1)).thenReturn(messageBatchResult);
		doCallRealMethod().when(spy).updateRecipient(messageResult, recipient1);

		final var completableFuture = spy.sendDigitalMailToRecipient(messageEntity, recipient1);

		completableFuture.join();
		assertThat(recipient1.getStatus()).isEqualTo(MessageStatus.SENT.toString());
		assertThat(recipient1.getExternalId()).isEqualTo(uuid.toString());
		verify(messagingIntegrationMock).sendDigitalMail(messageEntity, recipient1);
		verify(recipientRepositoryMock).save(recipient1);
	}

	@Test
	void sendDigitalMailToRecipient_exception() {
		final var spy = Mockito.spy(messageService);
		final var recipient1 = new RecipientEntity().withFirstName("john");
		final var messageEntity = MessageEntity.create().withRecipients(List.of(recipient1));

		when(messagingIntegrationMock.sendDigitalMail(messageEntity, recipient1)).thenThrow(new RuntimeException("Simulated exception"));

		final var completableFuture = spy.sendDigitalMailToRecipient(messageEntity, recipient1);

		completableFuture.join();
		assertThat(recipient1.getStatus()).isEqualTo(MessageStatus.FAILED.toString());
		assertThat(recipient1.getStatusDetail()).isEqualTo("java.lang.RuntimeException: Simulated exception");
		verify(messagingIntegrationMock).sendDigitalMail(messageEntity, recipient1);
		verify(recipientRepositoryMock).save(recipient1);
	}

	@Test
	void sendSnailMailToRecipient_success() {
		final var spy = Mockito.spy(messageService);
		final var recipient1 = new RecipientEntity().withFirstName("john");
		final var messageEntity = MessageEntity.create().withRecipients(List.of(recipient1));
		final var uuid = UUID.randomUUID();
		final var messageResult = new MessageResult()
			.messageId(uuid)
			.deliveries(List.of(new DeliveryResult()
				.status(MessageStatus.SENT)));

		when(messagingIntegrationMock.sendSnailMail(messageEntity, recipient1)).thenReturn(messageResult);
		doCallRealMethod().when(spy).updateRecipient(messageResult, recipient1);

		final var completableFuture = spy.sendSnailMailToRecipient(messageEntity, recipient1);

		completableFuture.join();
		assertThat(recipient1.getStatus()).isEqualTo(MessageStatus.SENT.toString());
		assertThat(recipient1.getExternalId()).isEqualTo(uuid.toString());
		verify(messagingIntegrationMock).sendSnailMail(messageEntity, recipient1);
		verify(recipientRepositoryMock).save(recipient1);
	}

	@Test
	void sendSnailMailToRecipient_exception() {
		final var spy = Mockito.spy(messageService);
		final var recipient1 = new RecipientEntity().withFirstName("john");
		final var messageEntity = MessageEntity.create().withRecipients(List.of(recipient1));

		when(messagingIntegrationMock.sendSnailMail(messageEntity, recipient1)).thenThrow(new RuntimeException("Simulated exception"));

		final var completableFuture = spy.sendSnailMailToRecipient(messageEntity, recipient1);

		completableFuture.join();
		assertThat(recipient1.getStatus()).isEqualTo(MessageStatus.FAILED.toString());
		assertThat(recipient1.getStatusDetail()).isEqualTo("java.lang.RuntimeException: Simulated exception");
		verify(messagingIntegrationMock).sendSnailMail(messageEntity, recipient1);
		verify(recipientRepositoryMock).save(recipient1);
	}

	@Test
	void getOrCreateUser_userExists() {
		final var userEntity = new UserEntity().withUsername("Linus");
		when(userRepositoryMock.findByUsernameIgnoreCase("Linus")).thenReturn(Optional.of(userEntity));

		final var result = messageService.getOrCreateUser("Linus");

		assertThat(result).isEqualTo(userEntity);
		verify(userRepositoryMock).findByUsernameIgnoreCase("Linus");
	}

	@Test
	void getOrCreateUser_userDoesNotExist() {
		when(userRepositoryMock.findByUsernameIgnoreCase("Linus")).thenReturn(Optional.empty());

		final var result = messageService.getOrCreateUser("Linus");

		assertThat(result).isNotNull().isInstanceOf(UserEntity.class);
		assertThat(result.getUsername()).isEqualTo("Linus");

		verify(userRepositoryMock).findByUsernameIgnoreCase("Linus");
	}

	@Test
	void getOrCreateDepartment_departmentExists() {
		final var departmentEntity = new DepartmentEntity().withName(SETTINGS_MAP.get(DEPARTMENT_NAME)).withOrganizationId(SETTINGS_MAP.get(DEPARTMENT_ID));
		when(departmentRepositoryMock.findByOrganizationId(SETTINGS_MAP.get(DEPARTMENT_ID))).thenReturn(Optional.of(departmentEntity));

		final var result = messageService.getOrCreateDepartment(SETTINGS_MAP);

		assertThat(result).isEqualTo(departmentEntity);
		verify(departmentRepositoryMock).findByOrganizationId(SETTINGS_MAP.get(DEPARTMENT_ID));
	}

	@Test
	void getOrCreateDepartment_departmentDoesNotExist() {
		var settingsMap = Map.of(DEPARTMENT_ID, "orgId", DEPARTMENT_NAME, "IT");
		when(departmentRepositoryMock.findByOrganizationId("orgId")).thenReturn(Optional.empty());

		final var result = messageService.getOrCreateDepartment(settingsMap);

		assertThat(result).isNotNull().isInstanceOf(DepartmentEntity.class);
		assertThat(result.getOrganizationId()).isEqualTo("orgId");
		assertThat(result.getName()).isEqualTo("IT");

		verify(departmentRepositoryMock).findByOrganizationId("orgId");
	}

}
