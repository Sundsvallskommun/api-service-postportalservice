package se.sundsvall.postportalservice.apptest.rabbitmq;

import static se.sundsvall.postportalservice.integration.rabbitmq.model.Constants.DIGITAL_REGISTERED_LETTER_EXCHANGE;
import static se.sundsvall.postportalservice.integration.rabbitmq.model.Constants.POST_PORTAL_SERVICE_EXCHANGE;
import static se.sundsvall.postportalservice.integration.rabbitmq.model.Constants.SEND_DIGITAL_REGISTERED_LETTER_QUEUE;
import static se.sundsvall.postportalservice.integration.rabbitmq.model.Constants.STATUS_DIGITAL_REGISTERED_LETTER_QUEUE;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.RabbitMQContainer;

/**
 * Starts a shared RabbitMQ Testcontainer and injects connection properties into the Spring context. Used by all integration tests via {@code @ContextConfiguration(initializers = RabbitMQContainerInitializer.class)}.
 * <p>
 * The container is started once (static) and reused across all test classes that reference this initializer, since they share the same Spring application context.
 */
public class RabbitMQContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:4-management");

	static {
		RABBIT.start();
		setupExchangesAndQueues();
	}

	@Override
	public void initialize(final ConfigurableApplicationContext context) {
		TestPropertyValues.of(
				"spring.rabbitmq.host=" + RABBIT.getHost(),
				"spring.rabbitmq.port=" + RABBIT.getAmqpPort(),
				"spring.rabbitmq.username=" + RABBIT.getAdminUsername(),
				"spring.rabbitmq.password=" + RABBIT.getAdminPassword())
			.applyTo(context.getEnvironment());
	}

	private static void setupExchangesAndQueues() {
		var factory = new com.rabbitmq.client.ConnectionFactory();
		factory.setHost(RABBIT.getHost());
		factory.setPort(RABBIT.getAmqpPort());
		factory.setUsername(RABBIT.getAdminUsername());
		factory.setPassword(RABBIT.getAdminPassword());

		try (var connection = factory.newConnection();
			var channel = connection.createChannel()) {
			channel.exchangeDeclare(POST_PORTAL_SERVICE_EXCHANGE, "topic", true);
			channel.exchangeDeclare(DIGITAL_REGISTERED_LETTER_EXCHANGE, "topic", true);
			channel.queueDeclare(SEND_DIGITAL_REGISTERED_LETTER_QUEUE, true, false, false, null);
			channel.queueDeclare(STATUS_DIGITAL_REGISTERED_LETTER_QUEUE, true, false, false, null);
			channel.queueBind(SEND_DIGITAL_REGISTERED_LETTER_QUEUE, POST_PORTAL_SERVICE_EXCHANGE, SEND_DIGITAL_REGISTERED_LETTER_QUEUE);
			channel.queueBind(STATUS_DIGITAL_REGISTERED_LETTER_QUEUE, DIGITAL_REGISTERED_LETTER_EXCHANGE, STATUS_DIGITAL_REGISTERED_LETTER_QUEUE);
		} catch (Exception e) {
			throw new RuntimeException("Failed to set up RabbitMQ exchanges/queues", e);
		}
	}
}
