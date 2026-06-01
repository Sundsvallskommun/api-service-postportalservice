package se.sundsvall.postportalservice.configuration;

import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Defines the thread pool used to fan out deliveries to the messaging service. The pool size is the concurrency limit -
 * the blocking messaging call runs on the pool thread, so the limit governs the resource that actually blocks.
 */
@Configuration
@EnableConfigurationProperties(DeliveryExecutorConfiguration.DeliveryExecutorProperties.class)
public class DeliveryExecutorConfiguration {

	public static final String DELIVERY_EXECUTOR = "letterDeliveryExecutor";

	@Bean(name = DELIVERY_EXECUTOR, destroyMethod = "shutdown")
	ThreadPoolTaskExecutor letterDeliveryExecutor(final DeliveryExecutorProperties properties, final MdcTaskDecorator mdcTaskDecorator) {
		final var executor = new ThreadPoolTaskExecutor();

		executor.setCorePoolSize(properties.poolSize());
		executor.setMaxPoolSize(properties.poolSize());
		executor.setQueueCapacity(Integer.MAX_VALUE);

		executor.setThreadNamePrefix("letter-delivery-");
		executor.setTaskDecorator(mdcTaskDecorator);

		// Graceful shutdown, tries to handle in-flight messages before shutting down.
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(properties.awaitTerminationSeconds());

		executor.initialize();
		return executor;
	}

	/**
	 * Exposes the delivery pool as Micrometer metrics (queue depth, active threads, pool size, ...) under the
	 * {@code letter.delivery} prefix, e.g. at {@code /actuator/metrics/executor.queued?tag=name:letter.delivery}.
	 */
	@Bean
	ExecutorServiceMetrics letterDeliveryExecutorMetrics(@Qualifier(DELIVERY_EXECUTOR) final ThreadPoolTaskExecutor letterDeliveryExecutor) {
		return new ExecutorServiceMetrics(letterDeliveryExecutor.getThreadPoolExecutor(), "letter.delivery", List.of());
	}

	/**
	 * Configuration for the delivery pool. {@code poolSize} is the concurrency limit towards messaging - the blocking
	 * call runs on the pool thread. The work queue is unbounded: a recipient is never rejected, at the cost of growing
	 * memory (and potentially OOM) under sustained overload.
	 */
	@ConfigurationProperties("delivery.executor")
	public record DeliveryExecutorProperties(
		@DefaultValue("8") int poolSize,
		@DefaultValue("30") int awaitTerminationSeconds) {
	}
}
