package se.sundsvall.postportalservice.apptest.support;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static se.sundsvall.postportalservice.apptest.support.MessagingQueueStub.STATUS_BINDING_PATTERN;
import static se.sundsvall.postportalservice.apptest.support.MessagingQueueStub.STATUS_EXCHANGE;
import static se.sundsvall.postportalservice.apptest.support.MessagingQueueStub.STATUS_QUEUE;
import static se.sundsvall.postportalservice.apptest.support.MessagingQueueStub.WORK_EXCHANGE;
import static se.sundsvall.postportalservice.apptest.support.MessagingQueueStub.WORK_QUEUE;
import static se.sundsvall.postportalservice.apptest.support.MessagingQueueStub.WORK_ROUTING_KEY;

/**
 * Declares the topology the messaging-topology-operator owns in the cluster, so the container starts from the same
 * shape the service publishes into.
 * <p>
 * These declarations exist only here. The application itself must never declare topology - its AMQP user has an empty
 * {@code configure} permission - so keeping them in test scope is what makes the test a check of the real contract
 * rather than of a topology the service invented for itself.
 */
@TestConfiguration
public class RabbitTestTopologyConfiguration {

	@Bean
	TopicExchange workExchange() {
		return new TopicExchange(WORK_EXCHANGE);
	}

	@Bean
	Queue workQueue() {
		return QueueBuilder.durable(WORK_QUEUE).build();
	}

	@Bean
	Binding workBinding() {
		return BindingBuilder.bind(workQueue()).to(workExchange()).with(WORK_ROUTING_KEY);
	}

	@Bean
	TopicExchange statusExchange() {
		return new TopicExchange(STATUS_EXCHANGE);
	}

	@Bean
	Queue statusQueue() {
		return QueueBuilder.durable(STATUS_QUEUE).build();
	}

	@Bean
	Binding statusBinding() {
		// sms.* catches sms.sent and sms.failed, and any later sms.<outcome>.
		return BindingBuilder.bind(statusQueue()).to(statusExchange()).with(STATUS_BINDING_PATTERN);
	}

	@Bean
	MessagingQueueStub messagingQueueStub(final RabbitTemplate rabbitTemplate) {
		return new MessagingQueueStub(rabbitTemplate);
	}
}
