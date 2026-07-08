package se.sundsvall.postportalservice.api.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.CoreMatchers.allOf;

class SignedDocumentTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void testBean() {
		org.hamcrest.MatcherAssert.assertThat(SignedDocument.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderPattern() {
		final var document = SignedDocument.create()
			.withName("Contract")
			.withFileName("signed.pdf")
			.withMimeType("application/pdf")
			.withContent("c2lnbmVk");

		assertThat(document.getName()).isEqualTo("Contract");
		assertThat(document.getFileName()).isEqualTo("signed.pdf");
		assertThat(document.getMimeType()).isEqualTo("application/pdf");
		assertThat(document.getContent()).isEqualTo("c2lnbmVk");
		assertThat(document).hasNoNullFieldsOrProperties();
	}

	@Test
	void validateEmptyBean() {
		assertThat(validator.validate(new SignedDocument()))
			.extracting(violation -> violation.getPropertyPath().toString(), ConstraintViolation::getMessage)
			.containsExactlyInAnyOrder(tuple("content", "must not be blank"));
	}

	@Test
	void validatePopulatedBean() {
		assertThat(validator.validate(SignedDocument.create().withContent("c2lnbmVk"))).isEmpty();
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(SignedDocument.create()).hasAllNullFieldsOrProperties();
		assertThat(new SignedDocument()).hasAllNullFieldsOrProperties();
	}
}
