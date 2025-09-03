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
import org.junit.jupiter.api.Test;

class DigitalRegisteredLetterRequestTest {

	private final String partyId = "6d0773d6-3e7f-4552-81bc-f0007af95adf";
	private final String subject = "This is the subject of the letter";

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void testBean() {
		org.hamcrest.MatcherAssert.assertThat(DigitalRegisteredLetterRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderPattern() {
		final var request = DigitalRegisteredLetterRequest.create()
			.withPartyId(partyId)
			.withSubject(subject);

		assertThat(request.getPartyId()).isEqualTo(partyId);
		assertThat(request.getSubject()).isEqualTo(subject);
		assertThat(request).hasNoNullFieldsOrProperties();
	}

	@Test
	void settersAndGetters() {
		final var request = new DigitalRegisteredLetterRequest();
		request.setPartyId(partyId);
		request.setSubject(subject);

		assertThat(request.getPartyId()).isEqualTo(partyId);
		assertThat(request.getSubject()).isEqualTo(subject);
		assertThat(request).hasNoNullFieldsOrProperties();
	}

	@Test
	void validateEmptyBean() {
		final var request = new DigitalRegisteredLetterRequest();

		final var violations = validator.validate(request);

		assertThat(violations).hasSize(2)
			.extracting(violation -> violation.getPropertyPath().toString(), ConstraintViolation::getMessage)
			.containsExactlyInAnyOrder(
				tuple("partyId", "not a valid UUID"),
				tuple("subject", "must not be blank"));
		assertThat(request).hasAllNullFieldsOrProperties();
	}

	@Test
	void validatePopulatedBean() {
		final var request = DigitalRegisteredLetterRequest.create()
			.withPartyId(partyId)
			.withSubject(subject);

		final var violations = validator.validate(request);

		assertThat(violations).isEmpty();
		assertThat(request).hasNoNullFieldsOrProperties();
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(DigitalRegisteredLetterRequest.create()).hasAllNullFieldsOrProperties();
		assertThat(new DigitalRegisteredLetterRequest()).hasAllNullFieldsOrProperties();
	}
}
