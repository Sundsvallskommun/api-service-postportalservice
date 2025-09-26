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
import static se.sundsvall.postportalservice.Constants.FAILED;
import static se.sundsvall.postportalservice.Constants.SENT;
import static se.sundsvall.postportalservice.TestDataFactory.MUNICIPALITY_ID;

import generated.se.sundsvall.employee.PortalPersonData;
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
import se.sundsvall.postportalservice.api.model.Attachments;
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
import se.sundsvall.postportalservice.integration.employee.EmployeeIntegration;
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
	private EmployeeIntegration employeeIntegrationMock;

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
			messagingIntegrationMock, employeeIntegrationMock,
			departmentRepositoryMock, userRepositoryMock,
			messageRepositoryMock, digitalRegisteredLetterIntegrationMock);
	}

	/**
	 * Employee integration returns empty optional, we expect a Problem (502 Bad Gateway) to be thrown and the process to
	 * stop.
	 */
	@Test
	void processDigitalRegisteredLetterRequest_employeeNotFound() throws Exception {
		var request = TestDataFactory.createValidDigitalRegisteredLetterRequest();
		var attachments = TestDataFactory.createValidAttachments(2);

		when(employeeIntegrationMock.getPortalPersonData(MUNICIPALITY_ID, USERNAME)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> messageService.processDigitalRegisteredLetterRequest(MUNICIPALITY_ID, request, attachments))
			.isInstanceOf(Problem.class)
			.hasMessage("Bad Gateway: Failed to retrieve employee data for user [%s]".formatted(USERNAME));

		verify(employeeIntegrationMock).getPortalPersonData(MUNICIPALITY_ID, USERNAME);
		verifyNoMoreInteractions(employeeIntegrationMock);
		verifyNoInteractions(messagingIntegrationMock, departmentRepositoryMock, userRepositoryMock, messageRepositoryMock);
	}

	/**
	 * Employee integration returns invalid orgTree. We expect the EmployeeUtil to return an empty optional, which results
	 * in a Problem (500 Internal Server Error) to be thrown and we expect the process to stop.
	 */
	@Test
	void processDigitalRegisteredLetterRequest_invalidOrgTree() throws Exception {
		var request = TestDataFactory.createValidDigitalRegisteredLetterRequest();
		var attachments = TestDataFactory.createValidAttachments(2);
		var personData = new PortalPersonData()
			.orgTree("invalid-org-tree-format");

		when(employeeIntegrationMock.getPortalPersonData(MUNICIPALITY_ID, USERNAME)).thenReturn(Optional.of(personData));

		assertThatThrownBy(() -> messageService.processDigitalRegisteredLetterRequest(MUNICIPALITY_ID, request, attachments))
			.isInstanceOf(Problem.class)
			.hasMessage("Internal Server Error: Failed to parse organization from employee data");

		verify(employeeIntegrationMock).getPortalPersonData(MUNICIPALITY_ID, USERNAME);
		verifyNoMoreInteractions(employeeIntegrationMock);
		verifyNoInteractions(messagingIntegrationMock, departmentRepositoryMock, userRepositoryMock, messageRepositoryMock);
	}

	/**
	 * Messaging settings integration throws an exception, we expect a Problem (502 Bad Gateway) to be thrown and the
	 * process to stop.
	 */
	@Test
	void processDigitalRegisteredLetterRequest_messagingSettingsThrows() throws Exception {
		var request = TestDataFactory.createValidDigitalRegisteredLetterRequest();
		var attachments = TestDataFactory.createValidAttachments(2);

		happyCaseStubEmployee();
		when(messagingSettingsIntegrationMock.getSenderInfo(eq(MUNICIPALITY_ID), any()))
			.thenThrow(Problem.valueOf(BAD_GATEWAY, "Found no sender info for departmentId"));

		assertThatThrownBy(() -> messageService.processDigitalRegisteredLetterRequest(MUNICIPALITY_ID, request, attachments))
			.isInstanceOf(Problem.class)
			.hasMessage("Bad Gateway: Found no sender info for departmentId");

		verify(employeeIntegrationMock).getPortalPersonData(MUNICIPALITY_ID, USERNAME);
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
	void processDigitalRegisteredLetterRequest_digitalRegisteredLetterThrows() throws Exception {
		var request = TestDataFactory.createValidDigitalRegisteredLetterRequest();
		var attachments = TestDataFactory.createValidAttachments(2);
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
		when(departmentRepositoryMock.findByOrganizationId("organizationId")).thenReturn(Optional.empty());
		when(attachmentMapperMock.toAttachmentEntities(attachments)).thenReturn(attachmentEntities);

		var result = messageService.processDigitalRegisteredLetterRequest(MUNICIPALITY_ID, request, attachments);

		assertThat(result).isNotNull().isEqualTo("messageId");

		verify(employeeIntegrationMock).getPortalPersonData(MUNICIPALITY_ID, USERNAME);
		verify(messagingSettingsIntegrationMock).getSenderInfo(eq(MUNICIPALITY_ID), any());
		verify(userRepositoryMock).findByName(USERNAME);
		verify(departmentRepositoryMock).findByOrganizationId("organizationId");
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
			assertThat(departmentEntity.getOrganizationId()).isEqualTo("organizationId");
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
	void processDigitalRegisteredLetterRequest_happyCase() throws Exception {
		var request = TestDataFactory.createValidDigitalRegisteredLetterRequest();
		var attachments = TestDataFactory.createValidAttachments(2);
		var attachmentEntities = List.of(new AttachmentEntity(), new AttachmentEntity());
		var userEntity = new UserEntity().withName(USERNAME).withId("userId");
		var departmentEntity = new DepartmentEntity().withName("departmentName").withOrganizationId("organizationId").withId("departmentId");

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
		when(departmentRepositoryMock.findByOrganizationId("organizationId")).thenReturn(Optional.of(departmentEntity));
		when(attachmentMapperMock.toAttachmentEntities(attachments)).thenReturn(attachmentEntities);

		var result = messageService.processDigitalRegisteredLetterRequest(MUNICIPALITY_ID, request, attachments);

		assertThat(result).isNotNull().isEqualTo("messageId");

		verify(employeeIntegrationMock).getPortalPersonData(MUNICIPALITY_ID, USERNAME);
		verify(messagingSettingsIntegrationMock).getSenderInfo(eq(MUNICIPALITY_ID), any());
		verify(userRepositoryMock).findByName(USERNAME);
		verify(departmentRepositoryMock).findByOrganizationId("organizationId");
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
			assertThat(departmentEntity.getOrganizationId()).isEqualTo("organizationId");
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
		var personData = new PortalPersonData()
			.orgTree("2|organizationId|departmentName¤3|1234|Test Avdelningar¤4|12345|Test Avdelningar Digital¤5|123456|Test Avdelningar Digitalisering¤6|1234567|Test Avdelning Digitalisering");
		when(employeeIntegrationMock.getPortalPersonData(any(), any()))
			.thenReturn(Optional.of(personData));
	}

	@Test
	void processLetterRequest() {
		var spy = Mockito.spy(messageService);
		var multipartFile = Mockito.mock(MultipartFile.class);
		var attachments = new Attachments().withFiles(List.of(multipartFile));
		var letterRequest = TestDataFactory.createValidLetterRequest();
		var sentBy = new MessageService.SentBy("username", "organizationId", "departmentName");
		var userEntity = new UserEntity().withName("username");
		var departmentEntity = new DepartmentEntity().withName("departmentName").withOrganizationId("organizationId");
		var attachmentEntity = new AttachmentEntity().withFileName("filename").withContentType("contentType").withContent(null);
		var messageId = "adc63e5c-b92f-4c75-b14f-819473cef5b6";

		when(attachmentMapperMock.toAttachmentEntities(attachments)).thenReturn(List.of(attachmentEntity));
		when(messagingSettingsIntegrationMock.getSenderInfo(MUNICIPALITY_ID, departmentEntity.getOrganizationId()))
			.thenReturn(new SenderInfoResponse()
				.supportText("supportText")
				.contactInformationUrl("contactInformationUrl")
				.contactInformationEmail("contactInformationEmail")
				.contactInformationPhoneNumber("contactInformationPhoneNumber"));

		doReturn(sentBy).when(spy).getSentBy(MUNICIPALITY_ID);
		doReturn(userEntity).when(spy).getOrCreateUser(sentBy.userName());
		doReturn(departmentEntity).when(spy).getOrCreateDepartment(sentBy);
		doReturn(new CompletableFuture<>()).when(spy).processRecipients(any());
		when(messageRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0, MessageEntity.class).withId(messageId));

		var result = spy.processLetterRequest(MUNICIPALITY_ID, letterRequest, attachments);

		assertThat(result).isEqualTo(messageId);
		verify(attachmentMapperMock).toAttachmentEntities(attachments);
		verify(entityMapperMock).toRecipientEntity(any(Address.class));
		verify(entityMapperMock).toRecipientEntity(any(Recipient.class));
		verify(spy).getSentBy(MUNICIPALITY_ID);
		verify(spy).getOrCreateUser(sentBy.userName());
		verify(spy).getOrCreateDepartment(sentBy);
		verify(spy).processRecipients(any());
		verify(messageRepositoryMock).save(any());
	}

	@Test
	void processSmsRequest() {
		var spy = Mockito.spy(messageService);
		var smsRequest = TestDataFactory.createValidSmsRequest();
		var sentBy = new MessageService.SentBy("username", "organizationId", "departmentName");
		var userEntity = new UserEntity().withName("username");
		var departmentEntity = new DepartmentEntity().withName("departmentName").withOrganizationId("organizationId");
		var messageId = "adc63e5c-b92f-4c75-b14f-819473cef5b6";

		when(messagingSettingsIntegrationMock.getSenderInfo(MUNICIPALITY_ID, departmentEntity.getOrganizationId()))
			.thenReturn(new SenderInfoResponse().smsSender("Avsändare"));
		doReturn(sentBy).when(spy).getSentBy(MUNICIPALITY_ID);
		doReturn(userEntity).when(spy).getOrCreateUser(sentBy.userName());
		doReturn(departmentEntity).when(spy).getOrCreateDepartment(sentBy);
		doReturn(new CompletableFuture<>()).when(spy).processRecipients(any());
		when(messageRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0, MessageEntity.class).withId(messageId));

		var result = spy.processSmsRequest(MUNICIPALITY_ID, smsRequest);

		assertThat(result).isEqualTo(messageId);
		verify(entityMapperMock).toRecipientEntity(any(SmsRecipient.class));
		verify(spy).getSentBy(MUNICIPALITY_ID);
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

		var sentBy = new MessageService.SentBy("username", "orgId", "IT");
		var result = messageService.getOrCreateDepartment(sentBy);

		assertThat(result).isEqualTo(departmentEntity);
		verify(departmentRepositoryMock).findByOrganizationId("orgId");
	}

	@Test
	void getOrCreateDepartment_departmentDoesNotExist() {
		when(departmentRepositoryMock.findByOrganizationId("orgId")).thenReturn(Optional.empty());

		var sentBy = new MessageService.SentBy("username", "orgId", "IT");
		var result = messageService.getOrCreateDepartment(sentBy);

		assertThat(result).isNotNull().isInstanceOf(DepartmentEntity.class);
		assertThat(result.getOrganizationId()).isEqualTo("orgId");
		assertThat(result.getName()).isEqualTo("IT");

		verify(departmentRepositoryMock).findByOrganizationId("orgId");
	}

	@Test
	void getSentBy() {
		var username = Identifier.get().getValue();

		var personData = new PortalPersonData()
			.orgTree("2|42|En man som heter Ove¤3|880|Sunes sommar¤4|1234|Solsidan¤5|8456|Sällskapsresan¤6|7894|Tomten är far till alla barnen");
		when(employeeIntegrationMock.getPortalPersonData(MUNICIPALITY_ID, username)).thenReturn(Optional.of(personData));

		var result = messageService.getSentBy(MUNICIPALITY_ID);

		assertThat(result).isNotNull().isInstanceOf(MessageService.SentBy.class);
		assertThat(result.userName()).isEqualTo(username);
		assertThat(result.organizationId()).isEqualTo("42");
		assertThat(result.departmentName()).isEqualTo("En man som heter Ove");
	}

	@Test
	void getSentBy_invalidOrgTree() {
		var username = Identifier.get().getValue();

		var personData = new PortalPersonData()
			.orgTree("invalid-org-tree-format");
		when(employeeIntegrationMock.getPortalPersonData(MUNICIPALITY_ID, username)).thenReturn(Optional.of(personData));

		assertThatThrownBy(() -> messageService.getSentBy(MUNICIPALITY_ID))
			.isInstanceOf(Problem.class)
			.hasMessage("Internal Server Error: Failed to parse organization from employee data");

		verify(employeeIntegrationMock).getPortalPersonData(MUNICIPALITY_ID, username);
	}

}
