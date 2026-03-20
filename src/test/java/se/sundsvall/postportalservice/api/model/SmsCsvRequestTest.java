package se.sundsvall.postportalservice.api.model;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.hamcrest.CoreMatchers.allOf;

class SmsCsvRequestTest {

	private final String message = "This is the message to be sent";

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void testBean() {
		org.hamcrest.MatcherAssert.assertThat(SmsCsvRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderPattern() {
		final var bean = SmsCsvRequest.create()
			.withMessage(message);

		assertThat(bean.getMessage()).isEqualTo(message);
	}

	@Test
	void validBean() {
		final var bean = SmsCsvRequest.create()
			.withMessage(message);

		final var violations = validator.validate(bean);
		assertThat(violations).isEmpty();
	}

	@Test
	void missingMessage() {
		final var bean = SmsCsvRequest.create();

		final var violations = validator.validate(bean);
		assertThat(violations)
			.extracting(v -> v.getPropertyPath().toString(), v -> v.getMessage())
			.containsExactly(tuple("message", "must not be blank"));
	}

	@Test
	void blankMessage() {
		final var bean = SmsCsvRequest.create()
			.withMessage(" ");

		final var violations = validator.validate(bean);
		assertThat(violations)
			.extracting(v -> v.getPropertyPath().toString(), v -> v.getMessage())
			.containsExactly(tuple("message", "must not be blank"));
	}
}
