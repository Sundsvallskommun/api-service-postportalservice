package se.sundsvall.postportalservice.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import se.sundsvall.postportalservice.api.validation.impl.ValidPdfConstraintValidator;

@Target({
	ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE
})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidPdfConstraintValidator.class)
public @interface ValidPdf {

	String message() default "content type must be application/pdf";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
