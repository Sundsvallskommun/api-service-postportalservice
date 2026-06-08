package se.sundsvall.postportalservice.api.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class PrecheckRequestTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void constructorAndAccessor() {
		final var partyIds = List.of("b46f0ca2-d2ad-43e8-8d50-3aeb949e3604");

		final var request = new PrecheckRequest(partyIds);

		assertThat(request.partyIds()).isEqualTo(partyIds);
	}

	@Test
	void validRequestHasNoViolations() {
		final var request = new PrecheckRequest(List.of("b46f0ca2-d2ad-43e8-8d50-3aeb949e3604"));

		assertThat(validator.validate(request)).isEmpty();
	}

	@Test
	void emptyListViolatesNotEmpty() {
		final var request = new PrecheckRequest(List.of());

		assertThat(validator.validate(request))
			.extracting(violation -> violation.getPropertyPath().toString(), ConstraintViolation::getMessage)
			.containsExactly(tuple("partyIds", "must not be empty"));
	}

	@Test
	void invalidUuidViolatesValidUuid() {
		final var request = new PrecheckRequest(List.of("not-a-uuid"));

		assertThat(validator.validate(request))
			.anySatisfy(violation -> assertThat(violation.getMessage()).isEqualTo("not a valid UUID"));
	}
}
