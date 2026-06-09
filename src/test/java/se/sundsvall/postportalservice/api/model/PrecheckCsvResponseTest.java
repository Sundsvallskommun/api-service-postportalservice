package se.sundsvall.postportalservice.api.model;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PrecheckCsvResponseTest {

	@Test
	void constructorAndAccessors() {
		final var duplicateEntries = Map.of("191111111111", 2);
		final var rejectedEntries = Set.of("200000000000");

		final var response = new PrecheckCsvResponse(duplicateEntries, rejectedEntries);

		assertThat(response.duplicateEntries()).isEqualTo(duplicateEntries);
		assertThat(response.rejectedEntries()).isEqualTo(rejectedEntries);
	}

	@Test
	void equalsAndHashCode() {
		final var first = new PrecheckCsvResponse(Map.of("a", 2), Set.of("b"));
		final var second = new PrecheckCsvResponse(Map.of("a", 2), Set.of("b"));

		assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
	}
}
