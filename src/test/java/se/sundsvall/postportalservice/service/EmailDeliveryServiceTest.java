package se.sundsvall.postportalservice.service;

import generated.se.sundsvall.messaging.MessageResult;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.postportalservice.integration.db.AttachmentEntity;
import se.sundsvall.postportalservice.integration.db.DepartmentEntity;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.db.UserEntity;
import se.sundsvall.postportalservice.integration.db.dao.RecipientRepository;
import se.sundsvall.postportalservice.integration.messaging.MessagingIntegration;
import se.sundsvall.postportalservice.integration.objectstore.ObjectStoreIntegration;
import se.sundsvall.postportalservice.integration.rabbitmq.EmailQueueMessage;
import se.sundsvall.postportalservice.integration.rabbitmq.EmailQueuePublisher;
import se.sundsvall.postportalservice.service.util.RecipientId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static se.sundsvall.postportalservice.Constants.PENDING;

@ExtendWith(MockitoExtension.class)
class EmailDeliveryServiceTest {

	private static final String RECIPIENT_ID = "8a2a0c66-8a4a-4a8b-9a91-b3b0e8dbb0f9";
	private static final String OBJECT_ID = "f8e2bd3c-1a6b-4f5e-9d0a-2c7b1e4f6a58";

	private static final Map<String, String> SETTINGS = Map.of(
		"callback_email", "callback@example.com",
		"callback_email_subject", "Subject");

	@Mock
	private MessagingIntegration messagingIntegrationMock;

	@Mock
	private RecipientRepository recipientRepositoryMock;

	@Mock
	private ObjectStoreIntegration objectStoreIntegrationMock;

	@Mock
	private EmailQueuePublisher emailQueuePublisherMock;

	@BeforeEach
	@AfterEach
	void clearRecipientId() {
		// RecipientId keeps a counter in a thread-local, and other tests in this JVM can leave it above zero, which
		// would make init() a no-op and leave a stale id in the MDC.
		while (RecipientId.get() != null) {
			RecipientId.reset();
		}
	}

	@Test
	void deliverEmail_restPathWhenQueueDisabled() {
		final var service = new EmailDeliveryService(messagingIntegrationMock, recipientRepositoryMock, attachmentUploadService(), Optional.empty());
		final var messageEntity = messageEntity();
		final var recipientEntity = recipientEntity();
		final var messageResult = new MessageResult().messageId(UUID.randomUUID());

		when(messagingIntegrationMock.sendCallbackEmail(messageEntity, recipientEntity, SETTINGS)).thenReturn(messageResult);

		final var result = service.deliverEmail(messageEntity, recipientEntity, SETTINGS);

		assertThat(result).isSameAs(messageResult);
		verify(messagingIntegrationMock).sendCallbackEmail(messageEntity, recipientEntity, SETTINGS);
		verifyNoMoreInteractions(messagingIntegrationMock);
		// Nothing is uploaded on this path: the REST call carries the bytes itself.
		verifyNoInteractions(recipientRepositoryMock, objectStoreIntegrationMock, emailQueuePublisherMock);
	}

	@Test
	void deliverEmail_queuePathUploadsAttachmentsAndSendsOnlyTheirIds() {
		final var service = queuePathService();
		final var messageEntity = messageEntity();
		final var recipientEntity = recipientEntity();

		when(objectStoreIntegrationMock.store(any(AttachmentEntity.class))).thenReturn(OBJECT_ID);

		final var result = service.deliverEmail(messageEntity, recipientEntity, SETTINGS);

		// No result to act on - the outcome arrives later on the status queue.
		assertThat(result).isNull();
		assertThat(recipientEntity.getStatus()).isEqualTo(PENDING);

		final var captor = ArgumentCaptor.forClass(EmailQueueMessage.class);
		verify(emailQueuePublisherMock).publish(captor.capture());
		final var attachment = captor.getValue().attachments().getFirst();
		assertThat(attachment.objectId()).isEqualTo(OBJECT_ID);
		assertThat(attachment.name()).isEqualTo("file.txt");
		assertThat(captor.getValue().recipientId()).isEqualTo(RECIPIENT_ID);
		verifyNoInteractions(messagingIntegrationMock);
	}

	@Test
	void deliverEmail_queuePathUploadsBeforeMarkingPending() {
		final var service = queuePathService();

		when(objectStoreIntegrationMock.store(any(AttachmentEntity.class))).thenReturn(OBJECT_ID);

		final var inOrder = inOrder(objectStoreIntegrationMock, recipientRepositoryMock, emailQueuePublisherMock);

		service.deliverEmail(messageEntity(), recipientEntity(), SETTINGS);

		// A store that will not take the attachment must fail the send outright rather than leave a recipient pending
		// on a message that was never published - and PENDING must be written before the publish, since the outcome
		// can arrive while we are still here.
		inOrder.verify(objectStoreIntegrationMock).store(any(AttachmentEntity.class));
		inOrder.verify(recipientRepositoryMock).save(any(RecipientEntity.class));
		inOrder.verify(emailQueuePublisherMock).publish(any());
	}

	@Test
	void deliverEmail_queuePathPropagatesUploadFailureWithoutPublishing() {
		final var service = queuePathService();

		doThrow(Problem.valueOf(BAD_GATEWAY, "store unavailable")).when(objectStoreIntegrationMock).store(any(AttachmentEntity.class));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.deliverEmail(messageEntity(), recipientEntity(), SETTINGS));

		verifyNoInteractions(emailQueuePublisherMock);
		verify(recipientRepositoryMock, never()).save(any());
	}

	@Test
	void deliverEmail_queuePathPropagatesPublishFailure() {
		final var service = queuePathService();

		when(objectStoreIntegrationMock.store(any(AttachmentEntity.class))).thenReturn(OBJECT_ID);
		doThrow(Problem.valueOf(BAD_GATEWAY, "no confirmation")).when(emailQueuePublisherMock).publish(any());

		// The caller is what turns this into a FAILED recipient.
		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.deliverEmail(messageEntity(), recipientEntity(), SETTINGS));

		verify(messagingIntegrationMock, never()).sendCallbackEmail(any(), any(), any());
	}

	@Test
	void deliverEmail_queuePathPutsRecipientIdInMdc() {
		final var service = queuePathService();

		when(objectStoreIntegrationMock.store(any(AttachmentEntity.class))).thenReturn(OBJECT_ID);

		service.deliverEmail(messageEntity(), recipientEntity(), SETTINGS);

		assertThat(RecipientId.get()).isEqualTo(RECIPIENT_ID);
	}

	private EmailDeliveryService queuePathService() {
		return new EmailDeliveryService(messagingIntegrationMock, recipientRepositoryMock, attachmentUploadService(), Optional.of(emailQueuePublisherMock));
	}

	// The upload service is real rather than mocked, so that the assertions below keep saying something about the
	// object store rather than about a stand-in for it. Its own memoisation is covered in AttachmentUploadServiceTest.
	private AttachmentUploadService attachmentUploadService() {
		return new AttachmentUploadService(objectStoreIntegrationMock);
	}

	private static MessageEntity messageEntity() {
		return MessageEntity.create()
			.withId("1a2b3c")
			.withMunicipalityId("2281")
			.withDisplayName("Sundsvall")
			.withBody("Hello")
			.withUser(UserEntity.create().withUsername("joe01doe"))
			.withDepartment(DepartmentEntity.create().withName("Department"))
			.withAttachments(List.of(AttachmentEntity.create()
				.withFileName("file.txt")
				.withContentType("text/plain")));
	}

	private static RecipientEntity recipientEntity() {
		return RecipientEntity.create()
			.withId(RECIPIENT_ID)
			.withPartyId("6d0773d6-3e7f-4552-81bc-f0007af95adf")
			.withStreetAddress("Gatan 1")
			.withZipCode("85230")
			.withCity("Sundsvall");
	}
}
