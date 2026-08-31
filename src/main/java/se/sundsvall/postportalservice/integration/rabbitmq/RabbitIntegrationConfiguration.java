package se.sundsvall.postportalservice.integration.rabbitmq;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring for the SMS queue path towards the messaging service.
 * <p>
 * The whole path is inert unless {@code rabbitmq.enabled=true}. Without these beans the connection factory stays lazy
 * and the application opens no AMQP connection at all, so the Feign fallback in
 * {@link se.sundsvall.postportalservice.service.MessageService} is what runs.
 * <p>
 * Nothing here declares topology. The AMQP user has an empty {@code configure} permission - exchanges, queues and
 * bindings are owned by the messaging-topology-operator via GitOps. Declaring anything (a {@code Declarable} bean, or
 * {@code queuesToDeclare} on a listener) fails with ACCESS_REFUSED rather than drifting from the declared topology.
 */
@Configuration
@ConditionalOnProperty(name = "rabbitmq.enabled", havingValue = "true")
@EnableConfigurationProperties(RabbitIntegrationConfiguration.RabbitIntegrationProperties.class)
public class RabbitIntegrationConfiguration {

	/**
	 * Serializes payloads as {@code application/json} in both directions. Spring Boot's auto-configured
	 * {@code RabbitTemplate} and listener container factory both pick up this single {@link MessageConverter} bean.
	 */
	@Bean
	MessageConverter jacksonMessageConverter() {
		return new JacksonJsonMessageConverter();
	}

	/**
	 * Connection settings ({@code spring.rabbitmq.host/port/virtual-host/username/password}) are deliberately absent
	 * from this repository - they are supplied per environment. Only the names of the topology objects, which are part
	 * of the contract with messaging, live here.
	 */
	@ConfigurationProperties("rabbitmq")
	public record RabbitIntegrationProperties(
		@DefaultValue("false") boolean enabled,
		@DefaultValue("api-fabriken.messaging") String exchange,
		@DefaultValue("sms") String routingKey,
		@DefaultValue("api-fabriken.postportal.sms-status") String statusQueue,
		@DefaultValue("5") int publishConfirmTimeoutSeconds) {
	}
}
