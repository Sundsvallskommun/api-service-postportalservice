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

class SmsRecipientTest {

	private final String partyId = "6d0773d6-3e7f-4552-81bc-f0007af95adf";
	private final String phoneNumber = "+46701234567";

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void testBean() {
		org.hamcrest.MatcherAssert.assertThat(SmsRecipient.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderPattern() {
		final var smsRecipient = SmsRecipient.create()
			.withPartyId(partyId)
			.withPhoneNumber(phoneNumber);

		assertThat(smsRecipient.getPartyId()).isEqualTo(partyId);
		assertThat(smsRecipient.getPhoneNumber()).isEqualTo(phoneNumber);
		assertThat(smsRecipient).hasNoNullFieldsOrProperties();
	}

	@Test
	void settersAndGetters() {
		final var smsRecipient = new SmsRecipient();
		smsRecipient.setPartyId(partyId);
		smsRecipient.setPhoneNumber(phoneNumber);

		assertThat(smsRecipient.getPartyId()).isEqualTo(partyId);
		assertThat(smsRecipient.getPhoneNumber()).isEqualTo(phoneNumber);
		assertThat(smsRecipient).hasNoNullFieldsOrProperties();
	}

	@Test
	void validateEmptyBean() {
		final var smsRecipient = new SmsRecipient();
		final var violations = validator.validate(smsRecipient);

		assertThat(violations).hasSize(2)
			.extracting(violation -> violation.getPropertyPath().toString(), ConstraintViolation::getMessage)
			.containsExactlyInAnyOrder(
				tuple("partyId", "not a valid UUID"),
				tuple("phoneNumber", "must be a valid MSISDN (example: +46701234567). Regular expression: ^\\+[1-9][\\d]{3,14}$"));
		assertThat(smsRecipient).hasAllNullFieldsOrProperties();
	}

	@Test
	void validatePopulatedBean() {
		final var bean = SmsRecipient.create()
			.withPartyId(partyId)
			.withPhoneNumber(phoneNumber);
		final var violations = validator.validate(bean);

		assertThat(violations).isEmpty();
		assertThat(bean).hasNoNullFieldsOrProperties();
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(SmsRecipient.create()).hasAllNullFieldsOrProperties();
		assertThat(new SmsRecipient()).hasAllNullFieldsOrProperties();
	}
}
