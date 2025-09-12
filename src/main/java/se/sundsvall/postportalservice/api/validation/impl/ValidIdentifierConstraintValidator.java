package se.sundsvall.postportalservice.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.postportalservice.api.validation.ValidIdentifier;

public class ValidIdentifierConstraintValidator implements ConstraintValidator<ValidIdentifier, String> {

	@Override
	public void initialize(ValidIdentifier constraintAnnotation) {
		ConstraintValidator.super.initialize(constraintAnnotation);
	}

	@Override
	public boolean isValid(final String value, final ConstraintValidatorContext context) {
		if (value == null) {
			return false;
		}

		// Parse header value to dept44 Identifier.
		var identifier = Identifier.parse(value);

		// If parsing failed, identifier will be null and the validation fails.
		if (identifier == null) {
			context.disableDefaultConstraintViolation();
			context.buildConstraintViolationWithTemplate("X-Sent-By must be in the format: [type=TYPE; VALUE]")
				.addConstraintViolation();
			return false;
		}

		// Only AD_ACCOUNT is valid.
		if (!identifier.getType().equals(Identifier.Type.AD_ACCOUNT)) {
			context.disableDefaultConstraintViolation();
			context.buildConstraintViolationWithTemplate("X-Sent-By must be of type [adAccount]")
				.addConstraintViolation();
			return false;
		}

		return true;
	}
}
