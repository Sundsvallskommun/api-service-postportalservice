package se.sundsvall.postportalservice.service.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.postportalservice.service.util.RecipientId.MDC_RECIPIENT_ID_KEY;

class RecipientIdTest {

	// Force a clean state both before and after each test so no state crosses test boundaries.
	@BeforeEach
	@AfterEach
	void resetState() {
		for (var i = 0; i < 100; i++) {
			RecipientId.reset();
		}
		MDC.clear();
	}

	@Test
	void initGeneratesIdWhenNoneProvided() {
		final var created = RecipientId.init();

		assertThat(created).isTrue();
		assertThat(RecipientId.get()).isNotNull();
		assertThat(MDC.get(MDC_RECIPIENT_ID_KEY)).isEqualTo(RecipientId.get());
	}

	@Test
	void initUsesProvidedId() {
		final var created = RecipientId.init("my-id");

		assertThat(created).isTrue();
		assertThat(RecipientId.get()).isEqualTo("my-id");
	}

	@Test
	void initWithBlankIdGeneratesId() {
		final var created = RecipientId.init("   ");

		assertThat(created).isTrue();
		assertThat(RecipientId.get()).isNotBlank();
		assertThat(RecipientId.get()).isNotEqualTo("   ");
	}

	@Test
	void nestedInitDoesNotOverwriteId() {
		RecipientId.init("outer");
		final var createdNested = RecipientId.init("inner");

		assertThat(createdNested).isFalse();
		assertThat(RecipientId.get()).isEqualTo("outer");
	}

	@Test
	void resetClearsOnlyWhenCounterReachesZero() {
		RecipientId.init("id");
		RecipientId.init("id"); // counter = 2

		assertThat(RecipientId.reset()).isFalse(); // counter = 1, id still set
		assertThat(RecipientId.get()).isEqualTo("id");

		assertThat(RecipientId.reset()).isTrue(); // counter = 0, cleared
		assertThat(RecipientId.get()).isNull();
	}

	@Test
	void resetWithoutInitReturnsFalse() {
		assertThat(RecipientId.reset()).isFalse();
	}
}
