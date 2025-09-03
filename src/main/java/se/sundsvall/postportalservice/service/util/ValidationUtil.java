package se.sundsvall.postportalservice.service.util;

public final class ValidationUtil {

	private ValidationUtil() {}

	public static <T> void validate(final T t) {
		try (var validatorFactory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
			var validator = validatorFactory.getValidator();
			var violations = validator.validate(t);
			if (!violations.isEmpty()) {
				throw new jakarta.validation.ConstraintViolationException(violations);
			}
		}
	}
}
