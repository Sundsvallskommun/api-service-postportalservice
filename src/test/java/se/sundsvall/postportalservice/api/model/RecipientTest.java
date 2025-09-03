package se.sundsvall.postportalservice.api.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.CoreMatchers.allOf;
import static se.sundsvall.postportalservice.TestDataFactory.createValidAddress;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class RecipientTest {

	private final String partyId = "6d0773d6-3e7f-4552-81bc-f0007af95adf";
	private final Recipient.DeliveryMethod deliveryMethod = Recipient.DeliveryMethod.DIGITAL_MAIL;
	private final Address address = createValidAddress();

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void testBean() {
		org.hamcrest.MatcherAssert.assertThat(Recipient.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderPattern() {
		final var recipient = Recipient.create()
			.withPartyId(partyId)
			.withDeliveryMethod(deliveryMethod)
			.withAddress(address);

		assertThat(recipient.getPartyId()).isEqualTo(partyId);
		assertThat(recipient.getDeliveryMethod()).isEqualTo(deliveryMethod);
		assertThat(recipient.getAddress()).isEqualTo(address);
		assertThat(recipient).hasNoNullFieldsOrProperties();
	}

	@Test
	void settersAndGetters() {
		final var recipient = new Recipient();
		recipient.setPartyId(partyId);
		recipient.setDeliveryMethod(deliveryMethod);
		recipient.setAddress(address);

		assertThat(recipient.getPartyId()).isEqualTo(partyId);
		assertThat(recipient.getDeliveryMethod()).isEqualTo(deliveryMethod);
		assertThat(recipient.getAddress()).isEqualTo(address);
		assertThat(recipient).hasNoNullFieldsOrProperties();
	}

	@Test
	void validateEmptyBean() {
		final var recipient = new Recipient();
		final var violations = validator.validate(recipient);

		assertThat(violations).hasSize(2)
			.extracting(violation -> violation.getPropertyPath().toString(), ConstraintViolation::getMessage)
			.containsExactlyInAnyOrder(
				tuple("partyId", "not a valid UUID"),
				tuple("deliveryMethod", "must not be null"));
		assertThat(recipient).hasAllNullFieldsOrProperties();
	}

	@Test
	void validatePopulatedBean() {
		final var recipient = Recipient.create()
			.withPartyId(partyId)
			.withDeliveryMethod(deliveryMethod)
			.withAddress(address);
		final var violations = validator.validate(recipient);

		assertThat(violations).isEmpty();
		assertThat(recipient).hasNoNullFieldsOrProperties();
	}
}
