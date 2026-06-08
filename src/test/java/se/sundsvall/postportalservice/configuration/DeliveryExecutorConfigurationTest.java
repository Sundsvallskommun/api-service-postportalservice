package se.sundsvall.postportalservice.configuration;

import org.junit.jupiter.api.Test;
import se.sundsvall.postportalservice.configuration.DeliveryExecutorConfiguration.DeliveryExecutorProperties;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryExecutorConfigurationTest {

	private final DeliveryExecutorConfiguration configuration = new DeliveryExecutorConfiguration();

	@Test
	void letterDeliveryExecutorIsConfiguredFromProperties() {
		final var properties = new DeliveryExecutorProperties(4, 15);

		final var executor = configuration.letterDeliveryExecutor(properties, new MdcTaskDecorator());

		assertThat(executor.getCorePoolSize()).isEqualTo(4);
		assertThat(executor.getMaxPoolSize()).isEqualTo(4);
		assertThat(executor.getThreadNamePrefix()).isEqualTo("letter-delivery-");
		executor.shutdown();
	}

	@Test
	void letterDeliveryExecutorMetricsIsCreated() {
		final var executor = configuration.letterDeliveryExecutor(new DeliveryExecutorProperties(4, 15), new MdcTaskDecorator());

		final var metrics = configuration.letterDeliveryExecutorMetrics(executor);

		assertThat(metrics).isNotNull();
		executor.shutdown();
	}

	@Test
	void deliveryExecutorPropertiesAccessors() {
		final var properties = new DeliveryExecutorProperties(8, 30);

		assertThat(properties.poolSize()).isEqualTo(8);
		assertThat(properties.awaitTerminationSeconds()).isEqualTo(30);
	}
}
