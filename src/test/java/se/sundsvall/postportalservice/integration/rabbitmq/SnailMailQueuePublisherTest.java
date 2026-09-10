package se.sundsvall.postportalservice.integration.rabbitmq;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static se.sundsvall.postportalservice.integration.rabbitmq.RabbitTestFixtures.EXCHANGE;
import static se.sundsvall.postportalservice.integration.rabbitmq.RabbitTestFixtures.OBJECT_ID;
import static se.sundsvall.postportalservice.integration.rabbitmq.RabbitTestFixtures.RECIPIENT_ID;
import static se.sundsvall.postportalservice.integration.rabbitmq.RabbitTestFixtures.properties;
import static se.sundsvall.postportalservice.integration.rabbitmq.RabbitTestFixtures.snailMailQueueMessage;

@ExtendWith(MockitoExtension.class)
class SnailMailQueuePublisherTest {

	private static final String ROUTING_KEY = "snail-mail";
	private static final SnailMailQueueMessage MESSAGE = snailMailQueueMessage();

	@Mock
	private RabbitTemplate rabbitTemplateMock;

	private SnailMailQueuePublisher publisher;

	@BeforeEach
	void setUp() {
		publisher = new SnailMailQueuePublisher(rabbitTemplateMock, properties());
	}

	@Test
	void publish() {
		confirmWith(new CorrelationData.Confirm(true, null), null);

		publisher.publish(MESSAGE);

		final var captor = ArgumentCaptor.forClass(SnailMailQueueMessage.class);
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
		doAnswer(_ -> null).when(rabbitTemplateMock)
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
	void publish_carriesAttachmentsByReferenceOnly() {
		confirmWith(new CorrelationData.Confirm(true, null), null);

		publisher.publish(MESSAGE);

		final var captor = ArgumentCaptor.forClass(SnailMailQueueMessage.class);
		verify(rabbitTemplateMock).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY), captor.capture(), any(CorrelationData.class));
		// What goes onto a quorum queue is an id, never the bytes: they would be replicated across three nodes, parked
		// in a wait queue for every retry, and kept in the parking lot after a give-up.
		assertThat(captor.getValue().attachments()).singleElement()
			.satisfies(attachment -> assertThat(attachment.objectId()).isEqualTo(OBJECT_ID));
	}

	private void confirmWith(final CorrelationData.Confirm confirm, final ReturnedMessage returnedMessage) {
		doAnswer(invocation -> {
			final var correlationData = invocation.getArgument(3, CorrelationData.class);
			correlationData.setReturned(returnedMessage);
			correlationData.getFuture().complete(confirm);
			return null;
		}).when(rabbitTemplateMock).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY), any(Object.class), any(CorrelationData.class));
	}
}
