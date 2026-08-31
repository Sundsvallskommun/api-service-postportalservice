package se.sundsvall.postportalservice.integration.rabbitmq;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.postportalservice.integration.rabbitmq.RabbitIntegrationConfiguration.RabbitIntegrationProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class SmsQueuePublisherTest {

	private static final String EXCHANGE = "api-fabriken.messaging";
	private static final String ROUTING_KEY = "sms";
	private static final String RECIPIENT_ID = "8a2a0c66-8a4a-4a8b-9a91-b3b0e8dbb0f9";

	private static final SmsQueueMessage MESSAGE = new SmsQueueMessage(
		"2281", "1a2b3c", RECIPIENT_ID, "6d0773d6-3e7f-4552-81bc-f0007af95adf",
		"+46701740605", "Sundsvall", "Department", "Hello", "joe01doe; type=adAccount", "PostPortalService");

	@Mock
	private RabbitTemplate rabbitTemplateMock;

	private SmsQueuePublisher publisher;

	@BeforeEach
	void setUp() {
		publisher = new SmsQueuePublisher(rabbitTemplateMock, new RabbitIntegrationProperties(
			true, EXCHANGE, ROUTING_KEY, "api-fabriken.postportal.sms-status", 1));
	}

	@Test
	void publish() {
		confirmWith(new CorrelationData.Confirm(true, null), null);

		publisher.publish(MESSAGE);

		final var captor = ArgumentCaptor.forClass(SmsQueueMessage.class);
		verify(rabbitTemplateMock).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY), captor.capture(), any(CorrelationData.class));
		verifyNoMoreInteractions(rabbitTemplateMock);
		assertThat(captor.getValue()).isEqualTo(MESSAGE);
	}

	@Test
	void publish_correlatesOnRecipientId() {
		confirmWith(new CorrelationData.Confirm(true, null), null);

		publisher.publish(MESSAGE);

		final var captor = ArgumentCaptor.forClass(CorrelationData.class);
		verify(rabbitTemplateMock).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY), any(Object.class), captor.capture());
		assertThat(captor.getValue().getId()).isEqualTo(RECIPIENT_ID);
	}

	@Test
	void publish_nack() {
		confirmWith(new CorrelationData.Confirm(false, "queue full"), null);

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> publisher.publish(MESSAGE))
			.withMessageContaining("was rejected by the broker")
			.withMessageContaining("queue full");
	}

	@Test
	void publish_unroutable() {
		final var returned = new ReturnedMessage(new Message("{}".getBytes(), new MessageProperties()), 312, "NO_ROUTE", EXCHANGE, ROUTING_KEY);
		confirmWith(new CorrelationData.Confirm(true, null), returned);

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> publisher.publish(MESSAGE))
			.withMessageContaining("was not routable")
			.withMessageContaining("NO_ROUTE");
	}

	@Test
	void publish_noConfirmWithinTimeout() {
		// Leave the future uncompleted, so the wait runs into the configured timeout.
		doAnswer(invocation -> null).when(rabbitTemplateMock)
			.convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY), any(Object.class), any(CorrelationData.class));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> publisher.publish(MESSAGE))
			.withMessageContaining("No broker confirmation");
	}

	@Test
	void publish_brokerUnreachable() {
		doThrow(new AmqpException("boom")).when(rabbitTemplateMock)
			.convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY), any(Object.class), any(CorrelationData.class));

		// Propagates rather than being swallowed - the caller is what marks the recipient FAILED.
		assertThatExceptionOfType(AmqpException.class).isThrownBy(() -> publisher.publish(MESSAGE));
	}

	@Test
	void publish_isNotFireAndForget() {
		confirmWith(new CorrelationData.Confirm(true, null), null);

		assertThatNoException().isThrownBy(() -> publisher.publish(MESSAGE));
	}

	private void confirmWith(final CorrelationData.Confirm confirm, final ReturnedMessage returnedMessage) {
		doAnswer(invocation -> {
			final var correlationData = invocation.getArgument(3, CorrelationData.class);
			correlationData.setReturned(returnedMessage);
			((CompletableFuture<CorrelationData.Confirm>) correlationData.getFuture()).complete(confirm);
			return null;
		}).when(rabbitTemplateMock).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY), any(Object.class), any(CorrelationData.class));
	}
}
