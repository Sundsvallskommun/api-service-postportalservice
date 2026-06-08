package se.sundsvall.postportalservice.api.validation.impl;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ValidDeliveryMethodConstraintValidatorTest {

	@Mock
	private ConstraintValidatorContext context;

	@InjectMocks
	private ValidDeliveryMethodConstraintValidator validator;

	@ParameterizedTest
	@ValueSource(strings = {
		"DIGITAL_MAIL", "SNAIL_MAIL", "DELIVERY_NOT_POSSIBLE"
	})
	void validValues(final String value) {
		assertThat(validator.isValid(value, context)).isTrue();
	}

	@Test
	void nullValueIsDeferredToNotNull() {
		assertThat(validator.isValid(null, context)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"INVALID", "digital_mail", "EMAIL"
	})
	void invalidValues(final String value) {
		assertThat(validator.isValid(value, context)).isFalse();
	}
}
