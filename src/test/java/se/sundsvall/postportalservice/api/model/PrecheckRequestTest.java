package se.sundsvall.postportalservice.api.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.CoreMatchers.allOf;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrecheckRequestTest {

	private final List<String> recipients = List.of("6d0773d6-3e7f-4552-81bc-f0007af95adf");

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void testBean() {
		org.hamcrest.MatcherAssert.assertThat(PrecheckRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderPattern() {
		final var precheckRequest = PrecheckRequest.create()
			.withRecipients(recipients);

		assertThat(precheckRequest.getRecipients()).isEqualTo(recipients);
		assertThat(precheckRequest).hasNoNullFieldsOrProperties();
	}

	@Test
	void settersAndGetters() {
		final var precheckRequest = new PrecheckRequest();
		precheckRequest.setRecipients(recipients);

		assertThat(precheckRequest.getRecipients()).isEqualTo(recipients);
		assertThat(precheckRequest).hasNoNullFieldsOrProperties();
	}

	@Test
	void validateEmptyBean() {
		final var request = new PrecheckRequest();

		var violations = validator.validate(request);

		assertThat(violations).hasSize(1)
			.extracting(violation -> violation.getPropertyPath().toString(), ConstraintViolation::getMessage)
			.containsExactlyInAnyOrder(tuple("recipients", "must not be empty"));
		assertThat(request).hasAllNullFieldsOrProperties();
	}

	@Test
	void validatePopulatedBean() {
		final var request = PrecheckRequest.create()
			.withRecipients(recipients);

		var violations = validator.validate(request);

		assertThat(violations).isEmpty();
		assertThat(request).hasNoNullFieldsOrProperties();
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(PrecheckRequest.create()).hasAllNullFieldsOrProperties();
		assertThat(new PrecheckRequest()).hasAllNullFieldsOrProperties();
	}

}
