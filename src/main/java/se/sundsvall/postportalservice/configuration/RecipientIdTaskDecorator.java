package se.sundsvall.postportalservice.configuration;

import org.jspecify.annotations.NonNull;
import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.async.MdcTaskDecorator;
import se.sundsvall.postportalservice.service.util.RecipientId;

/**
 * Adds {@link RecipientId} cleanup on top of dept44's {@link MdcTaskDecorator}, which propagates the SLF4J MDC and the
 * {@code Identifier} thread-local from the submitting thread to the pool thread that runs the task, and restores the
 * pool thread's previous context afterwards.
 * <p>
 * {@link RecipientId} keeps a counter in a thread-local of its own that dept44 knows nothing about, so it has to be
 * reset explicitly. The reset runs inside the delegated task, i.e. before dept44 restores the pool thread's previous
 * context, which preserves the ordering the previous service-local decorator had.
 * <p>
 * Being a {@link TaskDecorator} bean, this class suppresses the auto-configured dept44 decorator bean. That is
 * harmless, since it delegates to the very same implementation. Note that this service has no auto-configured
 * {@code applicationTaskExecutor} either: {@code letterDeliveryExecutor} is an {@code Executor} bean, which makes
 * Spring Boot's task execution auto-configuration back off entirely, so the delivery pool is the only executor there
 * is to decorate.
 */
@Component
public class RecipientIdTaskDecorator implements TaskDecorator {

	private final MdcTaskDecorator delegate = new MdcTaskDecorator();

	@Override
	public @NonNull Runnable decorate(final @NonNull Runnable runnable) {
		return delegate.decorate(() -> {
			try {
				runnable.run();
			} finally {
				RecipientId.reset();
			}
		});
	}
}
