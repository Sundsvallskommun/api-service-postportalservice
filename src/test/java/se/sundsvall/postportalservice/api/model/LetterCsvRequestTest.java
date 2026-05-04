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
import static org.assertj.core.groups.Tuple.tuple;
import static org.hamcrest.CoreMatchers.allOf;

class LetterCsvRequestTest {

	private final String subject = "This is the subject of the letter";
	private final String body = "This is the body of the letter";
	private final String contentType = "text/plain";

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void testBean() {
		org.hamcrest.MatcherAssert.assertThat(LetterCsvRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderPattern() {
		final var bean = LetterCsvRequest.create()
			.withSubject(subject)
			.withBody(body)
			.withContentType(contentType);

		assertThat(bean.getSubject()).isEqualTo(subject);
		assertThat(bean.getBody()).isEqualTo(body);
		assertThat(bean.getContentType()).isEqualTo(contentType);
		assertThat(bean).hasNoNullFieldsOrProperties();
	}

	@Test
	void settersAndGetters() {
		final var bean = new LetterCsvRequest();
		bean.setSubject(subject);
		bean.setBody(body);
		bean.setContentType(contentType);

		assertThat(bean.getSubject()).isEqualTo(subject);
		assertThat(bean.getBody()).isEqualTo(body);
		assertThat(bean.getContentType()).isEqualTo(contentType);
		assertThat(bean).hasNoNullFieldsOrProperties();
	}

	@Test
	void validateEmptyBean() {
		final var bean = new LetterCsvRequest();

		final var violations = validator.validate(bean);

		assertThat(violations).hasSize(3)
			.extracting(violation -> violation.getPropertyPath().toString(), ConstraintViolation::getMessage)
			.containsExactlyInAnyOrder(
				tuple("subject", "must not be blank"),
				tuple("body", "must not be blank"),
				tuple("contentType", "must not be blank"));
		assertThat(bean).hasAllNullFieldsOrProperties();
	}

	@Test
	void validatePopulatedBean() {
		final var bean = LetterCsvRequest.create()
			.withSubject(subject)
			.withBody(body)
			.withContentType(contentType);

		final var violations = validator.validate(bean);

		assertThat(violations).isEmpty();
		assertThat(bean).hasNoNullFieldsOrProperties();
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(LetterCsvRequest.create()).hasAllNullFieldsOrProperties();
		assertThat(new LetterCsvRequest()).hasAllNullFieldsOrProperties();
	}

}
