package se.sundsvall.postportalservice.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import se.sundsvall.postportalservice.api.model.Address;
import se.sundsvall.postportalservice.api.validation.ValidAddress;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class ValidAddressConstraintValidator implements ConstraintValidator<ValidAddress, Address> {

	@Override
	public boolean isValid(final Address value, final ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}
		final var hasPersonName = isNotBlank(value.getFirstName()) && isNotBlank(value.getLastName());
		final var hasOrganizationName = isNotBlank(value.getOrganizationName());
		return hasPersonName || hasOrganizationName;
	}
}
