package se.sundsvall.postportalservice.service.util;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;
import org.slf4j.MDC;

public final class SemaphoreUtil {

	private SemaphoreUtil() {
		// Prevent instantiation
	}

	/**
	 * Executes the given task within a semaphore permit, ensuring that the task is executed only when a permit is
	 * available. The semaphore is acquired before executing the task and released after the task completes, regardless of
	 * whether it completed
	 * successfully or exceptionally.
	 *
	 * @param  task      The task to be executed within the semaphore permit.
	 * @param  semaphore The semaphore to control access.
	 * @param  executor  The executor to run the task.
	 * @param  <T>       The type of the result produced by the task.
	 * @return           A CompletableFuture representing the result of the task.
	 */
	public static <T> CompletableFuture<T> withPermit(final Supplier<CompletableFuture<T>> task, final Semaphore semaphore, final Executor executor) {

		final var contextMap = MDC.getCopyOfContextMap();

		return CompletableFuture.runAsync(() -> {
			setMdc(contextMap);
			semaphore.acquireUninterruptibly();
		}, executor)
			.thenCompose(ignored -> {
				setMdc(contextMap);
				return task.get();
			})
			.whenComplete((res, ex) -> {
				try {
					semaphore.release();
				} finally {
					MDC.clear();
				}
			});
	}

	private static void setMdc(Map<String, String> contextMap) {
		if (contextMap == null) {
			MDC.clear();
		} else {
			MDC.setContextMap(contextMap);
		}
	}
}
