package se.sundsvall.postportalservice.configuration;

import java.util.HashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import se.sundsvall.postportalservice.service.util.RecipientId;

import static org.assertj.core.api.Assertions.assertThat;

class MdcTaskDecoratorTest {

	private final MdcTaskDecorator decorator = new MdcTaskDecorator();

	@BeforeEach
	void setUp() {
		// Guard against MDC/RecipientId leaking in from other tests on the shared thread - these tests assert on
		// absolute MDC state, so they must start from a clean context regardless of execution order.
		MDC.clear();
	}

	@AfterEach
	void tearDown() {
		MDC.clear();
	}

	@Test
	void propagatesContextToTaskThreadAndCleansUpAfter() {
		MDC.put("key", "value");
		final var seenInsideTask = new HashMap<String, String>();

		final var decorated = decorator.decorate(() -> seenInsideTask.put("key", MDC.get("key")));

		MDC.clear();
		decorated.run();

		assertThat(seenInsideTask).containsEntry("key", "value");
		assertThat(MDC.get("key")).isNull();
	}

	@Test
	void preservesExecutingThreadContextWhenTaskRunsInline() {
		MDC.put("x-request-id", "req-123");

		final var decorated = decorator.decorate(() -> MDC.put("x-recipient-id", "rec-1"));
		decorated.run();

		assertThat(MDC.get("x-request-id")).isEqualTo("req-123");
		assertThat(MDC.get("x-recipient-id")).isNull();
	}

	@Test
	void resetsRecipientIdThreadLocalAfterTask() {
		final var decorated = decorator.decorate(() -> RecipientId.init("recipient-1"));

		decorated.run();

		assertThat(RecipientId.get()).isNull();
		assertThat(MDC.get(RecipientId.MDC_RECIPIENT_ID_KEY)).isNull();
	}
}
