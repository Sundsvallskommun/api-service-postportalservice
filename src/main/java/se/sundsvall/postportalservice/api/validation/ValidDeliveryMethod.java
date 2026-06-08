package se.sundsvall.postportalservice.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import se.sundsvall.postportalservice.api.validation.impl.ValidDeliveryMethodConstraintValidator;

@Documented
@Target({
	ElementType.FIELD, ElementType.PARAMETER
})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidDeliveryMethodConstraintValidator.class)
public @interface ValidDeliveryMethod {

	String message() default "not a valid delivery method";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
