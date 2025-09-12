package se.sundsvall.postportalservice.api.validation.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.postportalservice.api.validation.ValidIdentifier;

@ExtendWith(MockitoExtension.class)
class ValidIdentifierConstraintValidatorTest {

	@Mock
	private ValidIdentifier mockAnnotation;

	@Mock
	private ConstraintValidatorContext context;

	@Mock
	private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

	@Mock
	private ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeContext;

	@InjectMocks
	private ValidIdentifierConstraintValidator validator;

	@BeforeEach
	void beforeEach() {
		validator.initialize(mockAnnotation);
	}

	@Test
	void nullValue() {
		var valid = validator.isValid(null, context);

		assertThat(valid).isFalse();
	}

	@Test
	void identifierParseFailed() {
		var headerValue = "invalidIdentifier";

		when(context.buildConstraintViolationWithTemplate(any())).thenReturn(violationBuilder);

		var valid = validator.isValid(headerValue, context);

		assertThat(valid).isFalse();

		verify(context).buildConstraintViolationWithTemplate("X-Sent-By must be in the format: [type=TYPE; VALUE]");
		verify(violationBuilder).addConstraintViolation();
	}

	@Test
	void invalidIdentifierType() {
		var headerValue = "type=INVALID; VALUE=someValue";

		when(context.buildConstraintViolationWithTemplate(any())).thenReturn(violationBuilder);

		var valid = validator.isValid(headerValue, context);

		assertThat(valid).isFalse();

		verify(context).buildConstraintViolationWithTemplate("X-Sent-By must be of type [adAccount]");
		verify(violationBuilder).addConstraintViolation();
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"type=adAccount; joe01doe",
		"type=adAccount; joe_doe",
		"joe01doe; type=adAccount",
		"joe_doe; type=adAccount",
	})
	void validIdentifiers(final String headerValue) {
		var valid = validator.isValid(headerValue, context);

		assertThat(valid).isTrue();
	}

}
