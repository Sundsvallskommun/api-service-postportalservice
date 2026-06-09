package se.sundsvall.postportalservice.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Optional;
import org.apache.commons.lang3.EnumUtils;
import se.sundsvall.postportalservice.api.validation.ValidDeliveryMethod;
import se.sundsvall.postportalservice.service.DeliveryMethod;

public class ValidDeliveryMethodConstraintValidator implements ConstraintValidator<ValidDeliveryMethod, String> {

	@Override
	public boolean isValid(final String value, final ConstraintValidatorContext context) {
		return Optional.ofNullable(value)
			.map(string -> EnumUtils.isValidEnum(DeliveryMethod.class, string))
			.orElse(true);
	}
}
