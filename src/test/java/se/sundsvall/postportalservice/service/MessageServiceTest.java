package se.sundsvall.postportalservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.BAD_GATEWAY;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static se.sundsvall.postportalservice.Constants.FAILED;
import static se.sundsvall.postportalservice.Constants.SENT;
import static se.sundsvall.postportalservice.TestDataFactory.MUNICIPALITY_ID;

import generated.se.sundsvall.messaging.DeliveryResult;
import generated.se.sundsvall.messaging.MessageResult;
import generated.se.sundsvall.messagingsettings.SenderInfoResponse;
import java.util.List;
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
import se.sundsvall.postportalservice.integration.db.converter.MessageStatus;
import se.sundsvall.postportalservice.integration.db.converter.MessageType;
import se.sundsvall.postportalservice.integration.db.dao.DepartmentRepository;
import se.sundsvall.postportalservice.integration.db.dao.MessageRepository;
import se.sundsvall.postportalservice.integration.db.dao.UserRepository;
import se.sundsvall.postportalservice.integration.digitalregisteredletter.DigitalRegisteredLetterIntegration;
import se.sundsvall.postportalservice.integration.messaging.MessagingIntegration;
import se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration;
import se.sundsvall.postportalservice.service.mapper.AttachmentMapper;
import se.sundsvall.postportalservice.service.mapper.EntityMapper;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

	private static final String USERNAME = "username";

	@Mock
	private MessagingIntegration messagingIntegrationMock;

	@Mock
	private EmployeeService employeeService;

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
	private MessagingSettingsIntegration messagingSettingsIntegrationMock;

	@Mock
	private DigitalRegisteredLetterIntegration digitalRegisteredLetterIntegrationMock;

	@Captor
	private ArgumentCaptor<MessageEntity> messageEntityCaptor;

	@InjectMocks
	private MessageService messageService;

	@BeforeEach
	void setup() {
		var identifier = Identifier.create()
			.withType(Identifier.Type.AD_ACCOUNT)
			.withValue(USERNAME)
			.withTypeString("AD_ACCOUNT");
		Identifier.set(identifier);
	}

	@AfterEach
	void tearDown() {
		Identifier.remove();
		verifyNoMoreInteractions(attachmentMapperMock, entityMapperMock,
			messagingIntegrationMock, employeeService,
			departmentRepositoryMock, userRepositoryMock,
			messageRepositoryMock, digitalRegisteredLetterIntegrationMock);
	}

	/**
	 * Employee service throws an 502 Problem, we expect the exception to be thrown and the process to stop.
	 */
	@Test
	void processDigitalRegisteredLetterRequest_employeeNotFound() {
		var request = TestDataFactory.createValidDigitalRegisteredLetterRequest();
		var multipartFile = Mockito.mock(MultipartFile.class);
		var multipartFiles = List.of(multipartFile);

		when(employeeService.getSentBy(MUNICIPALITY_ID)).thenThrow(Problem.valueOf(BAD_GATEWAY, "Failed to retrieve employee data for user [%s]".formatted(USERNAME)));

		assertThatThrownBy(() -> messageService.processDigitalRegisteredLetterRequest(MUNICIPALITY_ID, request, multipartFiles))
			.isInstanceOf(Problem.class)
			.hasMessage("Bad Gateway: Failed to retrieve employee data for user [%s]".formatted(USERNAME));

		verify(employeeService).getSentBy(MUNICIPALITY_ID);
		verifyNoMoreInteractions(employeeService);
		verifyNoInteractions(messagingIntegrationMock, departmentRepositoryMock, userRepositoryMock, messageRepositoryMock);
	}

	/**
	 * Employee service throws an 500 Problem, we expect the exception to be thrown and the process to stop.
	 */
	@Test
	void processDigitalRegisteredLetterRequest_invalidOrgTree() {
		var request = TestDataFactory.createValidDigitalRegisteredLetterRequest();
		var multipartFile = Mockito.mock(MultipartFile.class);
		var multipartFiles = List.of(multipartFile);

		when(employeeService.getSentBy(MUNICIPALITY_ID)).thenThrow(Problem.valueOf(INTERNAL_SERVER_ERROR, "Failed to parse organization from employee data"));

		assertThatThrownBy(() -> messageService.processDigitalRegisteredLetterRequest(MUNICIPALITY_ID, request, multipartFiles))
			.isInstanceOf(Problem.class)
			.hasMessage("Internal Server Error: Failed to parse organization from employee data");

		verify(employeeService).getSentBy(MUNICIPALITY_ID);
		verifyNoMoreInteractions(employeeService);
		verifyNoInteractions(messagingIntegrationMock, departmentRepositoryMock, userRepositoryMock, messageRepositoryMock);
	}

	/**
	 * Messaging settings integration throws an exception, we expect a Problem (502 Bad Gateway) to be thrown and the
	 * process to stop.
	 */
	@Test
	void processDigitalRegisteredLetterRequest_messagingSettingsThrows() {
		var request = TestDataFactory.createValidDigitalRegisteredLetterRequest();
		var multipartFile = Mockito.mock(MultipartFile.class);
		var multipartFiles = List.of(multipartFile);

		happyCaseStubEmployee();
		when(messagingSettingsIntegrationMock.getSenderInfo(eq(MUNICIPALITY_ID), any()))
			.thenThrow(Problem.valueOf(BAD_GATEWAY, "Found no sender info for departmentId"));

		assertThatThrownBy(() -> messageService.processDigitalRegisteredLetterRequest(MUNICIPALITY_ID, request, multipartFiles))
			.isInstanceOf(Problem.class)
			.hasMessage("Bad Gateway: Found no sender info for departmentId");

		verify(employeeService).getSentBy(MUNICIPALITY_ID);
		verify(messagingSettingsIntegrationMock).getSenderInfo(eq(MUNICIPALITY_ID), any());
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
		var request = TestDataFactory.createValidDigitalRegisteredLetterRequest();
		var multipartFile = Mockito.mock(MultipartFile.class);
		var multipartFiles = List.of(multipartFile);

		var attachmentEntities = List.of(new AttachmentEntity(), new AttachmentEntity());

		happyCaseStubEmployee();
		happyCaseStubMessagingSettings();
		when(messageRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0, MessageEntity.class).withId("messageId"));
		doAnswer(inv -> {
			var recipientEntity = inv.getArgument(1, RecipientEntity.class);
			recipientEntity.setStatus(FAILED);
			recipientEntity.setStatusDetail("Something when wrong");
			return null;
		}).when(digitalRegisteredLetterIntegrationMock).sendLetter(any(MessageEntity.class), any(RecipientEntity.class));
		when(userRepositoryMock.findByName(USERNAME)).thenReturn(Optional.empty());
		when(departmentRepositoryMock.findByOrganizationId("departmentId")).thenReturn(Optional.empty());
		when(attachmentMapperMock.toAttachmentEntities(multipartFiles)).thenReturn(attachmentEntities);

		var result = messageService.processDigitalRegisteredLetterRequest(MUNICIPALITY_ID, request, multipartFiles);

		assertThat(result).isNotNull().isEqualTo("messageId");

		verify(employeeService).getSentBy(MUNICIPALITY_ID);
		verify(messagingSettingsIntegrationMock).getSenderInfo(eq(MUNICIPALITY_ID), any());
		verify(userRepositoryMock).findByName(USERNAME);
		verify(departmentRepositoryMock).findByOrganizationId("departmentId");
		verify(digitalRegisteredLetterIntegrationMock).sendLetter(any(), any());
		verify(messageRepositoryMock).save(messageEntityCaptor.capture());
		var messageEntity = messageEntityCaptor.getValue();
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
			assertThat(userEntity.getName()).isEqualTo(USERNAME);
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
		var request = TestDataFactory.createValidDigitalRegisteredLetterRequest();
		var multipartFile = Mockito.mock(MultipartFile.class);
		var multipartFileList = List.of(multipartFile);
		var attachmentEntities = List.of(new AttachmentEntity(), new AttachmentEntity());
		var userEntity = new UserEntity().withName(USERNAME).withId("userId");
		var departmentEntity = new DepartmentEntity().withName("departmentName").withOrganizationId("departmentId").withId("departmentId");

		happyCaseStubEmployee();
		happyCaseStubMessagingSettings();

		when(messageRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0, MessageEntity.class).withId("messageId"));
		doAnswer(inv -> {
			var recipientEntity = inv.getArgument(1, RecipientEntity.class);
			recipientEntity.setStatus(SENT);
			recipientEntity.setExternalId("229a3e3e-17ae-423a-9a14-671b5b1bbd17");
			return null;
		}).when(digitalRegisteredLetterIntegrationMock).sendLetter(any(MessageEntity.class), any(RecipientEntity.class));
		when(userRepositoryMock.findByName(USERNAME)).thenReturn(Optional.of(userEntity));
		when(departmentRepositoryMock.findByOrganizationId("departmentId")).thenReturn(Optional.of(departmentEntity));
		when(attachmentMapperMock.toAttachmentEntities(multipartFileList)).thenReturn(attachmentEntities);

		var result = messageService.processDigitalRegisteredLetterRequest(MUNICIPALITY_ID, request, multipartFileList);

		assertThat(result).isNotNull().isEqualTo("messageId");

		verify(employeeService).getSentBy(MUNICIPALITY_ID);
		verify(messagingSettingsIntegrationMock).getSenderInfo(eq(MUNICIPALITY_ID), any());
		verify(userRepositoryMock).findByName(USERNAME);
		verify(departmentRepositoryMock).findByOrganizationId("departmentId");
		verify(digitalRegisteredLetterIntegrationMock).sendLetter(any(), any());
		verify(messageRepositoryMock).save(messageEntityCaptor.capture());
		var messageEntity = messageEntityCaptor.getValue();
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
			assertThat(userEntity.getName()).isEqualTo(USERNAME);
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

	private void happyCaseStubMessagingSettings() {
		when(messagingSettingsIntegrationMock.getSenderInfo(any(), any()))
			.thenReturn(new SenderInfoResponse()
				.supportText("supportText")
				.contactInformationUrl("contactInformationUrl")
				.contactInformationEmail("contactInformationEmail")
				.contactInformationPhoneNumber("contactInformationPhoneNumber")
				.organizationNumber("123455678"));
	}

	private void happyCaseStubEmployee() {
		var sentBy = new EmployeeService.SentBy("username", "departmentId", "departmentName");

		when(employeeService.getSentBy(any())).thenReturn(sentBy);
	}

	@Test
	void processLetterRequest() {
		var spy = Mockito.spy(messageService);
		var multipartFile = Mockito.mock(MultipartFile.class);
		var multipartFileList = List.of(multipartFile);
		var letterRequest = TestDataFactory.createValidLetterRequest();
		var sentBy = new EmployeeService.SentBy("username", "departmentId", "departmentName");
		var userEntity = new UserEntity().withName("username");
		var departmentEntity = new DepartmentEntity().withName("departmentName").withOrganizationId("departmentId");
		var attachmentEntity = new AttachmentEntity().withFileName("filename").withContentType("contentType").withContent(null);
		var messageId = "adc63e5c-b92f-4c75-b14f-819473cef5b6";

		when(attachmentMapperMock.toAttachmentEntities(multipartFileList)).thenReturn(List.of(attachmentEntity));
		when(messagingSettingsIntegrationMock.getSenderInfo(MUNICIPALITY_ID, departmentEntity.getOrganizationId()))
			.thenReturn(new SenderInfoResponse()
				.supportText("supportText")
				.contactInformationUrl("contactInformationUrl")
				.contactInformationEmail("contactInformationEmail")
				.contactInformationPhoneNumber("contactInformationPhoneNumber"));

		when(employeeService.getSentBy(MUNICIPALITY_ID)).thenReturn(sentBy);
		doReturn(userEntity).when(spy).getOrCreateUser(sentBy.userName());
		doReturn(departmentEntity).when(spy).getOrCreateDepartment(sentBy);
		doReturn(new CompletableFuture<>()).when(spy).processRecipients(any());
		when(messageRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0, MessageEntity.class).withId(messageId));

		var result = spy.processLetterRequest(MUNICIPALITY_ID, letterRequest, multipartFileList);

		assertThat(result).isEqualTo(messageId);
		verify(attachmentMapperMock).toAttachmentEntities(multipartFileList);
		verify(entityMapperMock).toRecipientEntity(any(Address.class));
		verify(entityMapperMock).toRecipientEntity(any(Recipient.class));
		verify(employeeService).getSentBy(MUNICIPALITY_ID);
		verify(spy).getOrCreateUser(sentBy.userName());
		verify(spy).getOrCreateDepartment(sentBy);
		verify(spy).processRecipients(any());
		verify(messageRepositoryMock).save(any());
	}

	@Test
	void processSmsRequest() {
		var spy = Mockito.spy(messageService);
		var smsRequest = TestDataFactory.createValidSmsRequest();
		var sentBy = new EmployeeService.SentBy("username", "departmentId", "departmentName");
		var userEntity = new UserEntity().withName("username");
		var departmentEntity = new DepartmentEntity().withName("departmentName").withOrganizationId("departmentId");
		var messageId = "adc63e5c-b92f-4c75-b14f-819473cef5b6";

		when(messagingSettingsIntegrationMock.getSenderInfo(MUNICIPALITY_ID, departmentEntity.getOrganizationId()))
			.thenReturn(new SenderInfoResponse().smsSender("Avsändare"));
		when(employeeService.getSentBy(MUNICIPALITY_ID)).thenReturn(sentBy);
		doReturn(userEntity).when(spy).getOrCreateUser(sentBy.userName());
		doReturn(departmentEntity).when(spy).getOrCreateDepartment(sentBy);
		doReturn(new CompletableFuture<>()).when(spy).processRecipients(any());
		when(messageRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0, MessageEntity.class).withId(messageId));

		var result = spy.processSmsRequest(MUNICIPALITY_ID, smsRequest);

		assertThat(result).isEqualTo(messageId);
		verify(entityMapperMock).toRecipientEntity(any(SmsRecipient.class));
		verify(employeeService).getSentBy(MUNICIPALITY_ID);
		verify(spy).getOrCreateUser(sentBy.userName());
		verify(spy).getOrCreateDepartment(sentBy);
		verify(spy).processRecipients(any());
		verify(messageRepositoryMock).save(any());
	}

	@Test
	void processRecipients() {
		var spy = Mockito.spy(messageService);
		var recipient1 = new RecipientEntity().withFirstName("john");
		var recipient2 = new RecipientEntity().withFirstName("sarah");

		var messageEntity = MessageEntity.create()
			.withRecipients(List.of(recipient1, recipient2));

		var future1 = new CompletableFuture<Void>();
		var future2 = new CompletableFuture<Void>();
		doReturn(future1).when(spy).sendMessageToRecipient(messageEntity, recipient1);
		doReturn(future2).when(spy).sendMessageToRecipient(messageEntity, recipient2);

		var completableFuture = spy.processRecipients(messageEntity);
		future1.complete(null);
		future2.complete(null);
		completableFuture.join();

		verify(spy).sendMessageToRecipient(messageEntity, recipient1);
		verify(spy).sendMessageToRecipient(messageEntity, recipient2);
		verify(messageRepositoryMock).save(messageEntity);
	}

	@Test
	void processRecipients_future_completes_exceptionally() {
		var spy = Mockito.spy(messageService);
		var recipient1 = new RecipientEntity().withFirstName("john");
		var recipient2 = new RecipientEntity().withFirstName("sarah");

		var messageEntity = MessageEntity.create()
			.withRecipients(List.of(recipient1, recipient2));

		var future1 = new CompletableFuture<Void>();
		var future2 = new CompletableFuture<Void>();
		doReturn(future1).when(spy).sendMessageToRecipient(messageEntity, recipient1);
		doReturn(future2).when(spy).sendMessageToRecipient(messageEntity, recipient2);

		var completableFuture = spy.processRecipients(messageEntity);

		future1.completeExceptionally(new RuntimeException("Simulated exception"));
		future2.complete(null);

		completableFuture.join();

		verify(spy, times(2)).sendMessageToRecipient(any(), any());
		verify(messageRepositoryMock).save(messageEntity);
	}

	@Test
	void processRecipients_future_delayed() {
		var spy = Mockito.spy(messageService);
		var recipient1 = new RecipientEntity().withFirstName("john");
		var recipient2 = new RecipientEntity().withFirstName("sarah");
		var messageEntity = MessageEntity.create().withRecipients(List.of(recipient1, recipient2));

		var future1 = new CompletableFuture<Void>();
		var future2 = new CompletableFuture<Void>();
		doReturn(future1).when(spy).sendMessageToRecipient(messageEntity, recipient1);
		doReturn(future2).when(spy).sendMessageToRecipient(messageEntity, recipient2);

		var completableFuture = spy.processRecipients(messageEntity);

		future1.complete(null);

		try {
			Thread.sleep(3000);
			future2.complete(null);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		completableFuture.join();

		verify(messageRepositoryMock, times(1)).save(messageEntity);
	}

	@Test
	void sendMessageToRecipient_SMS() {
		var spy = Mockito.spy(messageService);

		var recipient1 = new RecipientEntity().withFirstName("john").withMessageType(MessageType.SMS);
		var messageEntity = MessageEntity.create().withRecipients(List.of(recipient1));
		doReturn(new CompletableFuture<>()).when(spy).sendSmsToRecipient(messageEntity, recipient1);

		spy.sendMessageToRecipient(messageEntity, recipient1);

		verify(spy).sendSmsToRecipient(messageEntity, recipient1);
	}

	@Test
	void sendMessageToRecipient_digitalMail() {
		var spy = Mockito.spy(messageService);

		var recipient1 = new RecipientEntity().withFirstName("john").withMessageType(MessageType.DIGITAL_MAIL);
		var messageEntity = MessageEntity.create().withRecipients(List.of(recipient1));
		doReturn(new CompletableFuture<>()).when(spy).sendDigitalMailToRecipient(messageEntity, recipient1);

		spy.sendMessageToRecipient(messageEntity, recipient1);

		verify(spy).sendDigitalMailToRecipient(messageEntity, recipient1);
	}

	@Test
	void sendMessageToRecipient_snailMail() {
		var spy = Mockito.spy(messageService);

		var recipient1 = new RecipientEntity().withFirstName("john").withMessageType(MessageType.SNAIL_MAIL);
		var messageEntity = MessageEntity.create().withRecipients(List.of(recipient1));
		doReturn(new CompletableFuture<>()).when(spy).sendSnailMailToRecipient(messageEntity, recipient1);

		spy.sendMessageToRecipient(messageEntity, recipient1);

		verify(spy).sendSnailMailToRecipient(messageEntity, recipient1);
	}

	@Test
	void sendMessageToRecipient_unsupportedMessageType() {
		var recipient1 = new RecipientEntity().withFirstName("john").withMessageType(MessageType.LETTER);
		var messageEntity = MessageEntity.create().withRecipients(List.of(recipient1));

		messageService.sendMessageToRecipient(messageEntity, recipient1);

		assertThat(recipient1.getStatus()).isEqualTo(FAILED);
		assertThat(recipient1.getStatusDetail()).isEqualTo("Unsupported message type: LETTER");
	}

	@Test
	void processSmsRequestToRecipient() {
		var spy = Mockito.spy(messageService);
		var recipient1 = new RecipientEntity().withFirstName("john");
		var messageEntity = MessageEntity.create().withRecipients(List.of(recipient1));
		var uuid = UUID.randomUUID();
		var messageResult = new MessageResult()
			.messageId(uuid)
			.deliveries(List.of(new DeliveryResult()
				.status(generated.se.sundsvall.messaging.MessageStatus.SENT)));

		when(messagingIntegrationMock.sendSms(messageEntity, recipient1)).thenReturn(messageResult);
		doCallRealMethod().when(spy).updateRecipient(messageResult, recipient1);

		var completableFuture = spy.sendSmsToRecipient(messageEntity, recipient1);

		completableFuture.join();
		assertThat(recipient1.getStatus()).isEqualTo(MessageStatus.SENT.toString());
		assertThat(recipient1.getExternalId()).isEqualTo(uuid.toString());
		verify(messagingIntegrationMock).sendSms(messageEntity, recipient1);
	}

	@Test
	void processSmsRequestToRecipient_exception() {
		var spy = Mockito.spy(messageService);
		var recipient1 = new RecipientEntity().withFirstName("john");
		var messageEntity = MessageEntity.create().withRecipients(List.of(recipient1));

		when(messagingIntegrationMock.sendSms(messageEntity, recipient1)).thenThrow(new RuntimeException("Simulated exception"));

		var completableFuture = spy.sendSmsToRecipient(messageEntity, recipient1);

		completableFuture.join();
		assertThat(recipient1.getStatus()).isEqualTo(MessageStatus.FAILED.toString());
		assertThat(recipient1.getStatusDetail()).isEqualTo("java.lang.RuntimeException: Simulated exception");
		verify(messagingIntegrationMock).sendSms(messageEntity, recipient1);
	}

	@Test
	void getOrCreateUser_userExists() {
		var userEntity = new UserEntity().withName("Linus");
		when(userRepositoryMock.findByName("Linus")).thenReturn(Optional.of(userEntity));

		var result = messageService.getOrCreateUser("Linus");

		assertThat(result).isEqualTo(userEntity);
		verify(userRepositoryMock).findByName("Linus");
	}

	@Test
	void getOrCreateUser_userDoesNotExist() {
		when(userRepositoryMock.findByName("Linus")).thenReturn(Optional.empty());

		var result = messageService.getOrCreateUser("Linus");

		assertThat(result).isNotNull().isInstanceOf(UserEntity.class);
		assertThat(result.getName()).isEqualTo("Linus");

		verify(userRepositoryMock).findByName("Linus");
	}

	@Test
	void getOrCreateDepartment_departmentExists() {
		var departmentEntity = new DepartmentEntity().withName("IT").withOrganizationId("orgId");
		when(departmentRepositoryMock.findByOrganizationId("orgId")).thenReturn(Optional.of(departmentEntity));

		var sentBy = new EmployeeService.SentBy("username", "orgId", "IT");
		var result = messageService.getOrCreateDepartment(sentBy);

		assertThat(result).isEqualTo(departmentEntity);
		verify(departmentRepositoryMock).findByOrganizationId("orgId");
	}

	@Test
	void getOrCreateDepartment_departmentDoesNotExist() {
		when(departmentRepositoryMock.findByOrganizationId("orgId")).thenReturn(Optional.empty());

		var sentBy = new EmployeeService.SentBy("username", "orgId", "IT");
		var result = messageService.getOrCreateDepartment(sentBy);

		assertThat(result).isNotNull().isInstanceOf(DepartmentEntity.class);
		assertThat(result.getOrganizationId()).isEqualTo("orgId");
		assertThat(result.getName()).isEqualTo("IT");

		verify(departmentRepositoryMock).findByOrganizationId("orgId");
	}

}
