package se.sundsvall.postportalservice.api.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.CoreMatchers.allOf;

class EventSignatoryTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void testBean() {
		org.hamcrest.MatcherAssert.assertThat(EventSignatory.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderPattern() {
		final var signatory = EventSignatory.create()
			.withPartyId("6d0773d6-3e7f-4552-81bc-f0007af95adf")
			.withAction("APPROVED")
			.withReason("Signed off");

		assertThat(signatory.getPartyId()).isEqualTo("6d0773d6-3e7f-4552-81bc-f0007af95adf");
		assertThat(signatory.getAction()).isEqualTo("APPROVED");
		assertThat(signatory.getReason()).isEqualTo("Signed off");
		assertThat(signatory).hasNoNullFieldsOrProperties();
	}

	@Test
	void emptyBeanIsValid() {
		// All fields are optional; action only rejects unknown non-null values.
		assertThat(validator.validate(new EventSignatory())).isEmpty();
	}

	@Test
	void invalidActionIsRejected() {
		assertThat(validator.validate(EventSignatory.create().withAction("MAYBE")))
			.extracting(violation -> violation.getPropertyPath().toString(), ConstraintViolation::getMessage)
			.containsExactlyInAnyOrder(tuple("action", "The provided action is not a known signatory action."));
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(EventSignatory.create()).hasAllNullFieldsOrProperties();
		assertThat(new EventSignatory()).hasAllNullFieldsOrProperties();
	}
}
