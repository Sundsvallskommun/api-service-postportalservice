package se.sundsvall.postportalservice.configuration;

import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;
import se.sundsvall.postportalservice.service.util.RecipientId;

/**
 * Propagates the SLF4J MDC context from the submitting thread (typically the HTTP request thread) to the pool thread
 * that runs the task, and restores the executing thread's previous context afterward. Pool threads are reused, so the
 * {@code finally} block resets the {@link RecipientId} ThreadLocal and the MDC to avoid leaking context between tasks.
 */
@Component
public class MdcTaskDecorator implements TaskDecorator {

	@Override
	public @NonNull Runnable decorate(final @NonNull Runnable runnable) {
		// Captured on the submitting thread (e.g. the HTTP request thread)
		final var callerContext = MDC.getCopyOfContextMap();

		return () -> {
			final var previousContext = MDC.getCopyOfContextMap();
			setContext(callerContext);
			try {
				runnable.run();
			} finally {
				RecipientId.reset();
				setContext(previousContext);
			}
		};
	}

	private static void setContext(final Map<String, String> context) {
		if (context != null) {
			MDC.setContextMap(context);
		} else {
			MDC.clear();
		}
	}
}
