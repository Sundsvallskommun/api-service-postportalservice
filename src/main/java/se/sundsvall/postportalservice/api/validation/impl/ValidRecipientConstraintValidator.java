package se.sundsvall.postportalservice.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import java.util.Set;
import se.sundsvall.postportalservice.api.model.Recipient;
import se.sundsvall.postportalservice.api.validation.ValidRecipient;
import se.sundsvall.postportalservice.api.validation.groups.SnailMailGroup;

public class ValidRecipientConstraintValidator implements ConstraintValidator<ValidRecipient, Recipient> {

	@Override
	public void initialize(ValidRecipient constraintAnnotation) {
		ConstraintValidator.super.initialize(constraintAnnotation);
	}

	@Override
	public boolean isValid(final Recipient value, final ConstraintValidatorContext context) {
		if (value.getDeliveryMethod() == Recipient.DeliveryMethod.SNAIL_MAIL) {
			try (var factory = Validation.buildDefaultValidatorFactory()) {
				var validator = factory.getValidator();
				// Validates using SnailMailGroup
				var violations = validator.validate(value, SnailMailGroup.class);
				return buildConstraintViolation(violations, context);

			}
		}
		// Only require group validation for SNAIL_MAIL. For other delivery methods we rely on the default validation group.
		return true;
	}

	/**
	 * Builds constraint violation message and returns false if there are any violations. Otherwise, returns true.
	 *
	 * @param  violations constraint violations
	 * @param  context    constraint validator context
	 * @return            true if there are no violations, false otherwise
	 */
	boolean buildConstraintViolation(final Set<ConstraintViolation<Recipient>> violations, final ConstraintValidatorContext context) {
		if (!violations.isEmpty()) {
			context.disableDefaultConstraintViolation();
			for (var violation : violations) {
				context.buildConstraintViolationWithTemplate(violation.getMessage())
					.addPropertyNode(violation.getPropertyPath().toString())
					.addConstraintViolation();
			}
			return false;
		}
		return true;
	}
}
