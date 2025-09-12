package se.sundsvall.postportalservice.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import se.sundsvall.postportalservice.api.validation.impl.ValidIdentifierConstraintValidator;

@Target({
	ElementType.FIELD, ElementType.PARAMETER
})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidIdentifierConstraintValidator.class)
public @interface ValidIdentifier {

	String message() default "X-Sent-By must be provided and must be in the correct format [type=TYPE; VALUE]";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
