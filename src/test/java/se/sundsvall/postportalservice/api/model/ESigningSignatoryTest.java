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

class ESigningSignatoryTest {

	private final String partyId = "6d0773d6-3e7f-4552-81bc-f0007af95adf";
	private final String name = "John Doe";
	private final String email = "john.doe@sundsvall.se";

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void testBean() {
		org.hamcrest.MatcherAssert.assertThat(ESigningSignatory.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderPattern() {
		final var signatory = ESigningSignatory.create()
			.withPartyId(partyId)
			.withName(name)
			.withEmail(email);

		assertThat(signatory.getPartyId()).isEqualTo(partyId);
		assertThat(signatory.getName()).isEqualTo(name);
		assertThat(signatory.getEmail()).isEqualTo(email);
		assertThat(signatory).hasNoNullFieldsOrProperties();
	}

	@Test
	void settersAndGetters() {
		final var signatory = new ESigningSignatory();
		signatory.setPartyId(partyId);
		signatory.setName(name);
		signatory.setEmail(email);

		assertThat(signatory.getPartyId()).isEqualTo(partyId);
		assertThat(signatory.getName()).isEqualTo(name);
		assertThat(signatory.getEmail()).isEqualTo(email);
		assertThat(signatory).hasNoNullFieldsOrProperties();
	}

	@Test
	void validateEmptyBean() {
		final var violations = validator.validate(new ESigningSignatory());

		assertThat(violations)
			.extracting(violation -> violation.getPropertyPath().toString(), ConstraintViolation::getMessage)
			.containsExactlyInAnyOrder(
				tuple("partyId", "not a valid UUID"),
				tuple("name", "must not be blank"),
				tuple("email", "must not be blank"));
	}

	@Test
	void validatePopulatedBean() {
		final var signatory = ESigningSignatory.create()
			.withPartyId(partyId)
			.withName(name)
			.withEmail(email);

		assertThat(validator.validate(signatory)).isEmpty();
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(ESigningSignatory.create()).hasAllNullFieldsOrProperties();
		assertThat(new ESigningSignatory()).hasAllNullFieldsOrProperties();
	}
}
