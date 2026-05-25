package se.sundsvall.postportalservice.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import se.sundsvall.postportalservice.api.validation.impl.ValidAddressConstraintValidator;

@Documented
@Target({
	ElementType.TYPE, ElementType.TYPE_USE
})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidAddressConstraintValidator.class)
public @interface ValidAddress {

	String message() default "either firstName and lastName, or organizationName must be provided";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
