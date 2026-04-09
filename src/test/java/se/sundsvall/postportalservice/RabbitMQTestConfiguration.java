package se.sundsvall.postportalservice;

import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("junit")
public class RabbitMQTestConfiguration {

	@Bean
	RabbitTemplate rabbitTemplate() {
		return Mockito.mock(RabbitTemplate.class);
	}
}
