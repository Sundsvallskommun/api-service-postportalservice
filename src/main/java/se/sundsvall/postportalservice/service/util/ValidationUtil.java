package se.sundsvall.postportalservice.service.util;

import static jakarta.validation.Validation.buildDefaultValidatorFactory;

import jakarta.validation.ConstraintViolationException;

public final class ValidationUtil {

	private ValidationUtil() {}

	public static <T> void validate(final T t) {
		try (var validatorFactory = buildDefaultValidatorFactory()) {
			final var validator = validatorFactory.getValidator();
			final var violations = validator.validate(t);

			if (!violations.isEmpty()) {
				throw new ConstraintViolationException(violations);
			}
		}
	}
}
