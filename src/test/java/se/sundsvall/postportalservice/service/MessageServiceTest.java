package se.sundsvall.postportalservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.Problem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.postportalservice.TestDataFactory;
import se.sundsvall.postportalservice.integration.db.DepartmentEntity;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.db.UserEntity;
import se.sundsvall.postportalservice.integration.db.converter.MessageStatus;
import se.sundsvall.postportalservice.integration.db.converter.MessageType;
import se.sundsvall.postportalservice.integration.db.dao.DepartmentRepository;
import se.sundsvall.postportalservice.integration.db.dao.MessageRepository;
import se.sundsvall.postportalservice.integration.db.dao.UserRepository;
import se.sundsvall.postportalservice.integration.employee.EmployeeIntegration;
import se.sundsvall.postportalservice.integration.messaging.MessagingIntegration;
import se.sundsvall.postportalservice.integration.messagingsettings.MessagingSettingsIntegration;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

	@Mock
	private MessagingIntegration messagingIntegrationMock;

	@Mock
	private EmployeeIntegration employeeIntegrationMock;

	@Mock
	private DepartmentRepository departmentRepositoryMock;

	@Mock
	private UserRepository userRepositoryMock;

	@Mock
	private MessageRepository messageRepositoryMock;

	@Mock
	private MessagingSettingsIntegration messagingSettingsIntegrationMock;

	@InjectMocks
	private MessageService messageService;

	@BeforeEach
	void setup() {
		var identifier = Identifier.create()
			.withType(Identifier.Type.AD_ACCOUNT)
			.withValue("username")
			.withTypeString("AD_ACCOUNT");
		Identifier.set(identifier);
	}

	@AfterEach
	void tearDown() {
		Identifier.remove();
		verifyNoMoreInteractions(messagingIntegrationMock, employeeIntegrationMock, departmentRepositoryMock, userRepositoryMock, messageRepositoryMock);
	}

	@Test
	void processRequest() {
		var spy = Mockito.spy(messageService);
		var smsRequest = TestDataFactory.createValidSmsRequest();
		var sentBy = new MessageService.SentBy("username", "organizationId", "departmentName");
		var userEntity = new UserEntity().withName("username");
		var departmentEntity = new DepartmentEntity().withName("departmentName").withOrganizationId("organizationId");

		var messageEntity = new MessageEntity().withId("adc63e5c-b92f-4c75-b14f-819473cef5b6");

		when(messagingSettingsIntegrationMock.getSenderInfo(MUNICIPALITY_ID, departmentEntity.getOrganizationId()))
			.thenReturn(new SenderInfoResponse().smsSender("Avsändare"));
		doReturn(sentBy).when(spy).getSentBy(MUNICIPALITY_ID);
		doReturn(userEntity).when(spy).getOrCreateUser(sentBy.userName());
		doReturn(departmentEntity).when(spy).getOrCreateDepartment(sentBy);
		doReturn(new CompletableFuture<>()).when(spy).processRecipients(any());
		when(messageRepositoryMock.save(any())).thenReturn(messageEntity);

		var result = spy.processRequest(MUNICIPALITY_ID, smsRequest);

		assertThat(result).isEqualTo(messageEntity.getId());
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
	void sendMessageToRecipient_unsupportedMessageType() {
		var recipient1 = new RecipientEntity().withFirstName("john").withMessageType(MessageType.WEB_MESSAGE);
		var messageEntity = MessageEntity.create().withRecipients(List.of(recipient1));

		messageService.sendMessageToRecipient(messageEntity, recipient1);

		assertThat(recipient1.getMessageStatus()).isEqualTo(MessageStatus.FAILED);
		assertThat(recipient1.getStatusDetail()).isEqualTo("Unsupported message type: WEB_MESSAGE");
	}

	@Test
	void processRequestToRecipient() {
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
		assertThat(recipient1.getMessageStatus()).isEqualTo(MessageStatus.SENT);
		assertThat(recipient1.getMessagingId()).isEqualTo(uuid.toString());
		verify(messagingIntegrationMock).sendSms(messageEntity, recipient1);
	}

	@Test
	void processRequestToRecipient_exception() {
		var spy = Mockito.spy(messageService);
		var recipient1 = new RecipientEntity().withFirstName("john");
		var messageEntity = MessageEntity.create().withRecipients(List.of(recipient1));

		when(messagingIntegrationMock.sendSms(messageEntity, recipient1)).thenThrow(new RuntimeException("Simulated exception"));

		var completableFuture = spy.sendSmsToRecipient(messageEntity, recipient1);

		completableFuture.join();
		assertThat(recipient1.getMessageStatus()).isEqualTo(MessageStatus.FAILED);
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
