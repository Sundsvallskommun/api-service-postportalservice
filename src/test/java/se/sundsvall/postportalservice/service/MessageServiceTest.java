package se.sundsvall.postportalservice.service;

import generated.se.sundsvall.citizen.CitizenExtended;
import generated.se.sundsvall.messaging.DeliveryResult;
import generated.se.sundsvall.messaging.MessageBatchResult;
import generated.se.sundsvall.messaging.MessageResult;
import generated.se.sundsvall.messaging.MessageStatus;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.requestid.RequestId;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.postportalservice.TestDataFactory;
import se.sundsvall.postportalservice.api.model.Address;
import se.sundsvall.postportalservice.api.model.Recipient;
import se.sundsvall.postportalservice.api.model.SmsRecipient;
import se.sundsvall.postportalservice.integration.citizen.CitizenIntegration;
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
import se.sundsvall.postportalservice.integration.rabbitmq.Publisher;
import se.sundsvall.postportalservice.integration.rabbitmq.model.Queue;
import se.sundsvall.postportalservice.integration.rabbitmq.model.SendRegisteredLetterEvent;
import se.sundsvall.postportalservice.service.mapper.AttachmentMapper;
import se.sundsvall.postportalservice.service.mapper.EntityMapper;
import se.sundsvall.postportalservice.service.util.CsvUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static se.sundsvall.postportalservice.Constants.FAILED;
import static se.sundsvall.postportalservice.TestDataFactory.MUNICIPALITY_ID;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.CONTACT_INFORMATION_EMAIL;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.CONTACT_INFORMATION_PHONE_NUMBER;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.CONTACT_INFORMATION_URL;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.DEPARTMENT_ID;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.DEPARTMENT_NAME;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.FOLDER_NAME;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.ORGANIZATION_NUMBER;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.SMS_SENDER;
import static se.sundsvall.postportalservice.service.util.MessagingSettingsUtil.SUPPORT_TEXT;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

	private static final String USERNAME = "username";
	private static final String REQUEST_ID = "test-request-id";
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

	@Mock
	private CitizenIntegration citizenIntegrationMock;

	@Mock
	private Publisher publisherMock;

	@Captor
	private ArgumentCaptor<MessageEntity> messageEntityCaptor;

	@Captor
	private ArgumentCaptor<SendRegisteredLetterEvent> eventCaptor;

	@InjectMocks
	private MessageService messageService;

	@BeforeEach
	void setup() {
		final var identifier = Identifier.create()
			.withType(Identifier.Type.AD_ACCOUNT)
			.withValue(USERNAME)
			.withTypeString("AD_ACCOUNT");
		Identifier.set(identifier);
		RequestId.init(REQUEST_ID);
	}

	@AfterEach
	void tearDown() {
		Identifier.remove();
		RequestId.reset();
		verifyNoMoreInteractions(attachmentMapperMock, entityMapperMock,
			messagingIntegrationMock, messagingSettingsIntegrationMock,
			departmentRepositoryMock, userRepositoryMock,
			messageRepositoryMock, recipientRepositoryMock, digitalRegisteredLetterIntegrationMock,
			citizenIntegrationMock, publisherMock);
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
		verifyNoInteractions(messagingIntegrationMock, departmentRepositoryMock, userRepositoryMock, messageRepositoryMock, citizenIntegrationMock);
	}

	/**
	 * Verify that the message is saved and an event is published to RabbitMQ. The recipient should remain in PENDING status
	 * since processing is now async.
	 */
	@Test
	void processDigitalRegisteredLetterRequest_savesAndPublishesEvent() {
		final var request = TestDataFactory.createValidDigitalRegisteredLetterRequest();
		final var multipartFile = Mockito.mock(MultipartFile.class);
		final var multipartFiles = List.of(multipartFile);

		final var attachmentEntities = List.of(
			new AttachmentEntity().withId("attachment-id-1"),
			new AttachmentEntity().withId("attachment-id-2"));
		final var citizen = new CitizenExtended().givenname("John").lastname("Doe");

		when(messagingSettingsIntegrationMock.getMessagingSettingsForUser(MUNICIPALITY_ID)).thenReturn(SETTINGS_MAP);
		when(citizenIntegrationMock.getCitizens(MUNICIPALITY_ID, List.of(request.getPartyId()))).thenReturn(List.of(citizen));
		when(messageRepositoryMock.save(any())).thenAnswer(invocation -> {
			final var msg = invocation.getArgument(0, MessageEntity.class).withId("messageId");
			msg.getRecipients().getFirst().setId("recipientId");
			return msg;
		});
		when(userRepositoryMock.findByUsernameIgnoreCase(USERNAME)).thenReturn(Optional.empty());
		when(departmentRepositoryMock.findByOrganizationId("departmentId")).thenReturn(Optional.empty());
		when(attachmentMapperMock.toAttachmentEntities(multipartFiles)).thenReturn(attachmentEntities);

		final var result = messageService.processDigitalRegisteredLetterRequest(MUNICIPALITY_ID, request, multipartFiles);

		assertThat(result).isNotNull().isEqualTo("messageId");

		verify(citizenIntegrationMock).getCitizens(MUNICIPALITY_ID, List.of(request.getPartyId()));
		verify(userRepositoryMock).findByUsernameIgnoreCase(USERNAME);
		verify(departmentRepositoryMock).findByOrganizationId("departmentId");
		verify(messageRepositoryMock).save(messageEntityCaptor.capture());
		verify(publisherMock).publishEvent(eq(Queue.SEND_REGISTERED_LETTER), eventCaptor.capture());
		final var event = eventCaptor.getValue();
		assertThat(event.municipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(event.requestId()).isEqualTo(REQUEST_ID);
		assertThat(event.recipientId()).isEqualTo("recipientId");
		assertThat(event.sender().identifier()).isEqualTo(USERNAME);
		assertThat(event.sender().organizationNumber()).isEqualTo(SETTINGS_MAP.get(ORGANIZATION_NUMBER));
		assertThat(event.sender().organizationName()).isEqualTo("departmentName");
		assertThat(event.sender().supportText()).isEqualTo(SETTINGS_MAP.get(SUPPORT_TEXT));
		assertThat(event.sender().contactInformationUrl()).isEqualTo(SETTINGS_MAP.get(CONTACT_INFORMATION_URL));
		assertThat(event.sender().contactInformationEmail()).isEqualTo(SETTINGS_MAP.get(CONTACT_INFORMATION_EMAIL));
		assertThat(event.sender().contactInformationPhoneNumber()).isEqualTo(SETTINGS_MAP.get(CONTACT_INFORMATION_PHONE_NUMBER));
		assertThat(event.recipient().partyId()).isEqualTo(request.getPartyId());
		assertThat(event.message().subject()).isEqualTo(request.getSubject());
		assertThat(event.message().body()).isEqualTo(request.getBody());
		assertThat(event.message().contentType()).isEqualTo(request.getContentType());
		assertThat(event.message().attachmentIds()).containsExactly("attachment-id-1", "attachment-id-2");
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
			assertThat(recipientEntity.getFirstName()).isEqualTo("John");
			assertThat(recipientEntity.getLastName()).isEqualTo("Doe");
			assertThat(recipientEntity.getMessageType()).isEqualTo(MessageType.DIGITAL_REGISTERED_LETTER);
			assertThat(recipientEntity.getStatus()).isEqualTo("PENDING");
			assertThat(recipientEntity.getExternalId()).isNull();
		});

		verifyNoInteractions(messagingIntegrationMock, digitalRegisteredLetterIntegrationMock);
	}

	/**
	 * Happy case where everything works as expected. Message is saved and event is published.
	 */
	@Test
	void processDigitalRegisteredLetterRequest_happyCase() {
		final var request = TestDataFactory.createValidDigitalRegisteredLetterRequest();
		final var multipartFile = Mockito.mock(MultipartFile.class);
		final var multipartFileList = List.of(multipartFile);
		final var attachmentEntities = List.of(
			new AttachmentEntity().withId("attachment-id-1"),
			new AttachmentEntity().withId("attachment-id-2"));
		final var userEntity = new UserEntity().withUsername(USERNAME).withId("userId");
		final var departmentEntity = new DepartmentEntity().withName("departmentName").withOrganizationId("departmentId").withId("departmentId");
		final var citizen = new CitizenExtended().givenname("Jane").lastname("Smith");

		when(messagingSettingsIntegrationMock.getMessagingSettingsForUser(MUNICIPALITY_ID)).thenReturn(SETTINGS_MAP);
		when(citizenIntegrationMock.getCitizens(MUNICIPALITY_ID, List.of(request.getPartyId()))).thenReturn(List.of(citizen));
		when(messageRepositoryMock.save(any())).thenAnswer(invocation -> {
			final var msg = invocation.getArgument(0, MessageEntity.class).withId("messageId");
			msg.getRecipients().getFirst().setId("recipientId");
			return msg;
		});
		when(userRepositoryMock.findByUsernameIgnoreCase(USERNAME)).thenReturn(Optional.of(userEntity));
		when(departmentRepositoryMock.findByOrganizationId("departmentId")).thenReturn(Optional.of(departmentEntity));
		when(attachmentMapperMock.toAttachmentEntities(multipartFileList)).thenReturn(attachmentEntities);

		final var result = messageService.processDigitalRegisteredLetterRequest(MUNICIPALITY_ID, request, multipartFileList);

		assertThat(result).isNotNull().isEqualTo("messageId");

		verify(citizenIntegrationMock).getCitizens(MUNICIPALITY_ID, List.of(request.getPartyId()));
		verify(userRepositoryMock).findByUsernameIgnoreCase(USERNAME);
		verify(departmentRepositoryMock).findByOrganizationId("departmentId");
		verify(messageRepositoryMock).save(messageEntityCaptor.capture());
		verify(publisherMock).publishEvent(eq(Queue.SEND_REGISTERED_LETTER), eventCaptor.capture());
		final var event = eventCaptor.getValue();
		assertThat(event.municipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(event.requestId()).isEqualTo(REQUEST_ID);
		assertThat(event.recipientId()).isEqualTo("recipientId");
		assertThat(event.sender().identifier()).isEqualTo(USERNAME);
		assertThat(event.sender().organizationNumber()).isEqualTo(SETTINGS_MAP.get(ORGANIZATION_NUMBER));
		assertThat(event.sender().organizationName()).isEqualTo("departmentName");
		assertThat(event.sender().supportText()).isEqualTo(SETTINGS_MAP.get(SUPPORT_TEXT));
		assertThat(event.sender().contactInformationUrl()).isEqualTo(SETTINGS_MAP.get(CONTACT_INFORMATION_URL));
		assertThat(event.sender().contactInformationEmail()).isEqualTo(SETTINGS_MAP.get(CONTACT_INFORMATION_EMAIL));
		assertThat(event.sender().contactInformationPhoneNumber()).isEqualTo(SETTINGS_MAP.get(CONTACT_INFORMATION_PHONE_NUMBER));
		assertThat(event.recipient().partyId()).isEqualTo(request.getPartyId());
		assertThat(event.message().subject()).isEqualTo(request.getSubject());
		assertThat(event.message().body()).isEqualTo(request.getBody());
		assertThat(event.message().contentType()).isEqualTo(request.getContentType());
		assertThat(event.message().attachmentIds()).containsExactly("attachment-id-1", "attachment-id-2");
		final var messageEntity = messageEntityCaptor.getValue();
		assertThat(messageEntity).isNotNull();
		assertThat(messageEntity.getId()).isEqualTo("messageId");
		assertThat(messageEntity.getAttachments()).isEqualTo(attachmentEntities);
		assertThat(messageEntity.getDepartment()).isNotNull().isInstanceOf(DepartmentEntity.class).satisfies(entity -> {
			assertThat(departmentEntity.getId()).isEqualTo("departmentId");
			assertThat(departmentEntity.getName()).isEqualTo("departmentName");
			assertThat(departmentEntity.getOrganizationId()).isEqualTo("departmentId");
		});
		assertThat(messageEntity.getUser()).isNotNull().isInstanceOf(UserEntity.class).satisfies(entity -> {
			assertThat(userEntity.getId()).isEqualTo("userId");
			assertThat(userEntity.getUsername()).isEqualTo(USERNAME);
		});
		assertThat(messageEntity.getRecipients().getFirst()).isNotNull().isInstanceOf(RecipientEntity.class).satisfies(recipientEntity -> {
			assertThat(recipientEntity.getPartyId()).isEqualTo(request.getPartyId());
			assertThat(recipientEntity.getFirstName()).isEqualTo("Jane");
			assertThat(recipientEntity.getLastName()).isEqualTo("Smith");
			assertThat(recipientEntity.getMessageType()).isEqualTo(MessageType.DIGITAL_REGISTERED_LETTER);
			assertThat(recipientEntity.getStatus()).isEqualTo("PENDING");
			assertThat(recipientEntity.getExternalId()).isNull();
		});

		verifyNoInteractions(messagingIntegrationMock, digitalRegisteredLetterIntegrationMock);
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

		doReturn(new CompletableFuture<>()).when(spy).processRecipients(any(), any());
		when(messageRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0, MessageEntity.class).withId(messageId));

		final var result = spy.processLetterRequest(MUNICIPALITY_ID, letterRequest, multipartFileList);

		assertThat(result).isEqualTo(messageId);
		verify(attachmentMapperMock).toAttachmentEntities(multipartFileList);
		verify(messagingSettingsIntegrationMock).getMessagingSettingsForUser(MUNICIPALITY_ID);
		verify(userRepositoryMock).findByUsernameIgnoreCase(Identifier.get().getValue());
		verify(departmentRepositoryMock).findByOrganizationId(SETTINGS_MAP.get(DEPARTMENT_ID));
		verify(entityMapperMock).toRecipientEntity(any(Address.class));
		verify(entityMapperMock).toRecipientEntity(any(Recipient.class));
		verify(spy).processRecipients(any(), any());
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
		doReturn(new CompletableFuture<>()).when(spy).processRecipients(any(), any());
		when(messageRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0, MessageEntity.class).withId(messageId));

		final var result = spy.processSmsRequest(MUNICIPALITY_ID, smsRequest);

		assertThat(result).isEqualTo(messageId);
		verify(messagingSettingsIntegrationMock).getMessagingSettingsForUser(MUNICIPALITY_ID);
		verify(userRepositoryMock).findByUsernameIgnoreCase(Identifier.get().getValue());
		verify(departmentRepositoryMock).findByOrganizationId(SETTINGS_MAP.get(DEPARTMENT_ID));
		verify(entityMapperMock).toRecipientEntity(any(SmsRecipient.class));
		verify(spy).processRecipients(any(), any());
		verify(messageRepositoryMock).save(any());
	}

	@Test
	void processCsvSmsRequest() {
		final var spy = Mockito.spy(messageService);
		final var smsCsvRequest = TestDataFactory.createValidSmsCsvRequest();
		final var csvFile = Mockito.mock(MultipartFile.class);
		final var userEntity = new UserEntity().withUsername("username");
		final var departmentEntity = new DepartmentEntity().withName("departmentName").withOrganizationId("departmentId");
		final var messageId = "adc63e5c-b92f-4c75-b14f-819473cef5b6";

		final var validEntries = new LinkedHashMap<String, Integer>();
		validEntries.put("+46701740610", 1);
		validEntries.put("+46701740620", 1);
		final var validationResult = new CsvUtil.SmsCsvValidationResult(validEntries, Set.of());

		when(messagingSettingsIntegrationMock.getMessagingSettingsForUser(MUNICIPALITY_ID)).thenReturn(SETTINGS_MAP);
		when(userRepositoryMock.findByUsernameIgnoreCase(Identifier.get().getValue())).thenReturn(Optional.of(userEntity));
		when(departmentRepositoryMock.findByOrganizationId(SETTINGS_MAP.get(DEPARTMENT_ID))).thenReturn(Optional.of(departmentEntity));
		doReturn(new CompletableFuture<>()).when(spy).processRecipients(any(), eq(SETTINGS_MAP));
		when(messageRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0, MessageEntity.class).withId(messageId));

		try (MockedStatic<CsvUtil> csvUtilMock = Mockito.mockStatic(CsvUtil.class)) {
			csvUtilMock.when(() -> CsvUtil.validateSmsCsv(csvFile)).thenReturn(validationResult);

			final var result = spy.processCsvSmsRequest(MUNICIPALITY_ID, smsCsvRequest, csvFile);

			assertThat(result).isEqualTo(messageId);
			csvUtilMock.verify(() -> CsvUtil.validateSmsCsv(csvFile));
		}

		verify(messagingSettingsIntegrationMock).getMessagingSettingsForUser(MUNICIPALITY_ID);
		verify(userRepositoryMock).findByUsernameIgnoreCase(Identifier.get().getValue());
		verify(departmentRepositoryMock).findByOrganizationId(SETTINGS_MAP.get(DEPARTMENT_ID));
		verify(spy).processRecipients(messageEntityCaptor.capture(), eq(SETTINGS_MAP));
		verify(messageRepositoryMock).save(any());

		final var capturedMessage = messageEntityCaptor.getValue();
		assertThat(capturedMessage.getBody()).isEqualTo("This is a test message");
		assertThat(capturedMessage.getMessageType()).isEqualTo(MessageType.SMS);
		assertThat(capturedMessage.getRecipients()).hasSize(2);
		assertThat(capturedMessage.getRecipients()).allSatisfy(recipient -> {
			assertThat(recipient.getMessageType()).isEqualTo(MessageType.SMS);
			assertThat(recipient.getStatus()).isEqualTo("PENDING");
			assertThat(recipient.getPhoneNumber()).isIn("+46701740610", "+46701740620");
		});
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
		doReturn(future1).when(spy).sendMessageToRecipient(messageEntity, recipient1, SETTINGS_MAP);
		doReturn(future2).when(spy).sendMessageToRecipient(messageEntity, recipient2, SETTINGS_MAP);

		final var completableFuture = spy.processRecipients(messageEntity, SETTINGS_MAP);
		future1.complete(null);
		future2.complete(null);
		completableFuture.join();

		verify(spy).sendMessageToRecipient(messageEntity, recipient1, SETTINGS_MAP);
		verify(spy).sendMessageToRecipient(messageEntity, recipient2, SETTINGS_MAP);
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
		doReturn(future1).when(spy).sendMessageToRecipient(messageEntity, recipient1, SETTINGS_MAP);
		doReturn(future2).when(spy).sendMessageToRecipient(messageEntity, recipient2, SETTINGS_MAP);

		final var completableFuture = spy.processRecipients(messageEntity, SETTINGS_MAP);

		future1.completeExceptionally(new RuntimeException("Simulated exception"));
		future2.complete(null);

		completableFuture.join();

		verify(spy, times(2)).sendMessageToRecipient(any(), any(), any());
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
		doReturn(future1).when(spy).sendMessageToRecipient(messageEntity, recipient1, SETTINGS_MAP);
		doReturn(future2).when(spy).sendMessageToRecipient(messageEntity, recipient2, SETTINGS_MAP);

		final var completableFuture = spy.processRecipients(messageEntity, SETTINGS_MAP);

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

		spy.sendMessageToRecipient(messageEntity, recipient1, SETTINGS_MAP);

		verify(spy).sendSmsToRecipient(messageEntity, recipient1);
	}

	@Test
	void sendMessageToRecipient_digitalMail() {
		final var spy = Mockito.spy(messageService);

		final var recipient1 = new RecipientEntity().withFirstName("john").withMessageType(MessageType.DIGITAL_MAIL);
		final var messageEntity = MessageEntity.create().withRecipients(List.of(recipient1));
		doReturn(new CompletableFuture<>()).when(spy).sendDigitalMailToRecipient(messageEntity, recipient1);

		spy.sendMessageToRecipient(messageEntity, recipient1, SETTINGS_MAP);

		verify(spy).sendDigitalMailToRecipient(messageEntity, recipient1);
	}

	@Test
	void sendMessageToRecipient_snailMail() {
		final var spy = Mockito.spy(messageService);

		final var recipient1 = new RecipientEntity().withFirstName("john").withMessageType(MessageType.SNAIL_MAIL);
		final var messageEntity = MessageEntity.create().withRecipients(List.of(recipient1));
		doReturn(new CompletableFuture<>()).when(spy).sendSnailMailToRecipient(messageEntity, recipient1, SETTINGS_MAP);

		spy.sendMessageToRecipient(messageEntity, recipient1, SETTINGS_MAP);

		verify(spy).sendSnailMailToRecipient(messageEntity, recipient1, SETTINGS_MAP);
	}

	@Test
	void sendMessageToRecipient_unsupportedMessageType() {
		final var recipient1 = new RecipientEntity().withFirstName("john").withMessageType(MessageType.LETTER);
		final var messageEntity = MessageEntity.create().withRecipients(List.of(recipient1));

		messageService.sendMessageToRecipient(messageEntity, recipient1, SETTINGS_MAP);

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

		final var completableFuture = spy.sendSnailMailToRecipient(messageEntity, recipient1, SETTINGS_MAP);

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

		final var completableFuture = spy.sendSnailMailToRecipient(messageEntity, recipient1, SETTINGS_MAP);

		completableFuture.join();
		assertThat(recipient1.getStatus()).isEqualTo(MessageStatus.FAILED.toString());
		assertThat(recipient1.getStatusDetail()).isEqualTo("java.lang.RuntimeException: Simulated exception");
		verify(messagingIntegrationMock).sendSnailMail(messageEntity, recipient1);
		verify(recipientRepositoryMock).save(recipient1);
	}

	@Test
	void sendSnailMailToRecipient_callbackEmail_success() {
		final var spy = Mockito.spy(messageService);
		final var recipient1 = new RecipientEntity().withFirstName("john");
		final var messageEntity = MessageEntity.create().withRecipients(List.of(recipient1));
		final var uuid = UUID.randomUUID();
		final var messageResult = new MessageResult()
			.messageId(uuid)
			.deliveries(List.of(new DeliveryResult()
				.status(MessageStatus.SENT)));
		final var callbackSettingsMap = Map.of(
			"snailmail_method", "Callback_Email",
			"callback_email", "test@example.com",
			"callback_email_subject", "Subject");

		when(messagingIntegrationMock.sendCallbackEmail(messageEntity, recipient1, callbackSettingsMap)).thenReturn(messageResult);
		doCallRealMethod().when(spy).updateRecipient(messageResult, recipient1);

		final var completableFuture = spy.sendSnailMailToRecipient(messageEntity, recipient1, callbackSettingsMap);

		completableFuture.join();
		assertThat(recipient1.getStatus()).isEqualTo(MessageStatus.SENT.toString());
		assertThat(recipient1.getExternalId()).isEqualTo(uuid.toString());
		verify(messagingIntegrationMock).sendCallbackEmail(messageEntity, recipient1, callbackSettingsMap);
		verify(recipientRepositoryMock).save(recipient1);
	}

	@Test
	void sendSnailMailToRecipient_callbackEmail_exception() {
		final var spy = Mockito.spy(messageService);
		final var recipient1 = new RecipientEntity().withFirstName("john");
		final var messageEntity = MessageEntity.create().withRecipients(List.of(recipient1));
		final var callbackSettingsMap = Map.of(
			"snailmail_method", "Callback_Email",
			"callback_email", "test@example.com",
			"callback_email_subject", "Subject");

		when(messagingIntegrationMock.sendCallbackEmail(messageEntity, recipient1, callbackSettingsMap)).thenThrow(new RuntimeException("Simulated exception"));

		final var completableFuture = spy.sendSnailMailToRecipient(messageEntity, recipient1, callbackSettingsMap);

		completableFuture.join();
		assertThat(recipient1.getStatus()).isEqualTo(MessageStatus.FAILED.toString());
		assertThat(recipient1.getStatusDetail()).isEqualTo("java.lang.RuntimeException: Simulated exception");
		verify(messagingIntegrationMock).sendCallbackEmail(messageEntity, recipient1, callbackSettingsMap);
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
