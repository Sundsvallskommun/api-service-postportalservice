package se.sundsvall.postportalservice.api.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.CoreMatchers.allOf;
import static se.sundsvall.postportalservice.TestDataFactory.createValidSmsRecipient;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import org.junit.jupiter.api.Test;

class SmsRequestTest {

	private final String message = "This is the message to be sent";
	private final List<SmsRecipient> recipients = List.of(createValidSmsRecipient());

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void testBean() {
		org.hamcrest.MatcherAssert.assertThat(SmsRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderPattern() {
		final var smsRequest = SmsRequest.create()
			.withMessage(message)
			.withRecipients(recipients);

		assertThat(smsRequest.getMessage()).isEqualTo(message);
		assertThat(smsRequest.getRecipients()).isEqualTo(recipients);
		assertThat(smsRequest).hasNoNullFieldsOrProperties();
	}

	@Test
	void settersAndGetters() {
		final var smsRequest = new SmsRequest();
		smsRequest.setMessage(message);
		smsRequest.setRecipients(recipients);

		assertThat(smsRequest.getMessage()).isEqualTo(message);
		assertThat(smsRequest.getRecipients()).isEqualTo(recipients);
		assertThat(smsRequest).hasNoNullFieldsOrProperties();
	}

	@Test
	void validateEmptyBean() {
		final var smsRequest = new SmsRequest();
		final var violations = validator.validate(smsRequest);

		assertThat(violations).hasSize(2)
			.extracting(violation -> violation.getPropertyPath().toString(), ConstraintViolation::getMessage)
			.containsExactlyInAnyOrder(
				tuple("message", "must not be blank"),
				tuple("recipients", "must not be empty"));
		assertThat(smsRequest).hasAllNullFieldsOrProperties();
	}

	@Test
	void validatePopulatedBean() {
		final var smsRequest = SmsRequest.create()
			.withMessage(message)
			.withRecipients(recipients);
		final var violations = validator.validate(smsRequest);

		assertThat(violations).isEmpty();
		assertThat(smsRequest).hasNoNullFieldsOrProperties();
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(SmsRequest.create()).hasAllNullFieldsOrProperties();
		assertThat(new SmsRequest()).hasAllNullFieldsOrProperties();
	}
}
