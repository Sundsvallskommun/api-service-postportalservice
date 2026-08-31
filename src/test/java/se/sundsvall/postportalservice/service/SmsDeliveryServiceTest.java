package se.sundsvall.postportalservice.service;

import generated.se.sundsvall.messaging.MessageResult;
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
import se.sundsvall.postportalservice.integration.db.DepartmentEntity;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;
import se.sundsvall.postportalservice.integration.db.UserEntity;
import se.sundsvall.postportalservice.integration.db.dao.RecipientRepository;
import se.sundsvall.postportalservice.integration.messaging.MessagingIntegration;
import se.sundsvall.postportalservice.integration.rabbitmq.SmsQueueMessage;
import se.sundsvall.postportalservice.integration.rabbitmq.SmsQueuePublisher;
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
class SmsDeliveryServiceTest {

	private static final String RECIPIENT_ID = "8a2a0c66-8a4a-4a8b-9a91-b3b0e8dbb0f9";

	@Mock
	private MessagingIntegration messagingIntegrationMock;

	@Mock
	private RecipientRepository recipientRepositoryMock;

	@Mock
	private SmsQueuePublisher smsQueuePublisherMock;

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
	void deliverSms_restPathWhenQueueDisabled() {
		final var service = new SmsDeliveryService(messagingIntegrationMock, recipientRepositoryMock, Optional.empty());
		final var messageEntity = messageEntity();
		final var recipientEntity = recipientEntity();
		final var messageResult = new MessageResult().messageId(UUID.randomUUID());

		when(messagingIntegrationMock.sendSms(messageEntity, recipientEntity)).thenReturn(messageResult);

		final var result = service.deliverSms(messageEntity, recipientEntity);

		assertThat(result).isSameAs(messageResult);
		verify(messagingIntegrationMock).sendSms(messageEntity, recipientEntity);
		verifyNoMoreInteractions(messagingIntegrationMock);
		verifyNoInteractions(recipientRepositoryMock, smsQueuePublisherMock);
	}

	@Test
	void deliverSms_queuePathPublishesAndReportsNoResult() {
		final var service = queuePathService();
		final var messageEntity = messageEntity();
		final var recipientEntity = recipientEntity();

		final var result = service.deliverSms(messageEntity, recipientEntity);

		// No result to act on - the outcome arrives later on the status queue.
		assertThat(result).isNull();
		assertThat(recipientEntity.getStatus()).isEqualTo(PENDING);

		final var captor = ArgumentCaptor.forClass(SmsQueueMessage.class);
		verify(smsQueuePublisherMock).publish(captor.capture());
		assertThat(captor.getValue().recipientId()).isEqualTo(RECIPIENT_ID);
		assertThat(captor.getValue().mobileNumber()).isEqualTo("+46701740605");
		verify(recipientRepositoryMock).save(recipientEntity);
		verifyNoInteractions(messagingIntegrationMock);
	}

	@Test
	void deliverSms_queuePathMarksPendingBeforePublishing() {
		final var service = queuePathService();
		final var messageEntity = messageEntity();
		final var recipientEntity = recipientEntity();

		final var inOrder = inOrder(recipientRepositoryMock, smsQueuePublisherMock);

		service.deliverSms(messageEntity, recipientEntity);

		// A status message that lands while we are still here must not be overwritten by a later write of PENDING.
		inOrder.verify(recipientRepositoryMock).save(recipientEntity);
		inOrder.verify(smsQueuePublisherMock).publish(any());
	}

	@Test
	void deliverSms_queuePathPropagatesPublishFailure() {
		final var service = queuePathService();
		final var messageEntity = messageEntity();
		final var recipientEntity = recipientEntity();

		doThrow(Problem.valueOf(BAD_GATEWAY, "no confirmation")).when(smsQueuePublisherMock).publish(any());

		// The caller is what turns this into a FAILED recipient.
		assertThatExceptionOfType(ThrowableProblem.class).isThrownBy(() -> service.deliverSms(messageEntity, recipientEntity));

		verify(messagingIntegrationMock, never()).sendSms(any(), any());
	}

	@Test
	void deliverSms_queuePathPutsRecipientIdInMdc() {
		final var service = queuePathService();

		service.deliverSms(messageEntity(), recipientEntity());

		assertThat(RecipientId.get()).isEqualTo(RECIPIENT_ID);
	}

	private SmsDeliveryService queuePathService() {
		return new SmsDeliveryService(messagingIntegrationMock, recipientRepositoryMock, Optional.of(smsQueuePublisherMock));
	}

	private static MessageEntity messageEntity() {
		return MessageEntity.create()
			.withId("1a2b3c")
			.withMunicipalityId("2281")
			.withDisplayName("Sundsvall")
			.withBody("Hello")
			.withUser(UserEntity.create().withUsername("joe01doe"))
			.withDepartment(DepartmentEntity.create().withName("Department"));
	}

	private static RecipientEntity recipientEntity() {
		return RecipientEntity.create()
			.withId(RECIPIENT_ID)
			.withPartyId("6d0773d6-3e7f-4552-81bc-f0007af95adf")
			.withPhoneNumber("+46701740605");
	}
}
