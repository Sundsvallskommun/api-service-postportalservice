package se.sundsvall.postportalservice.api.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.CoreMatchers.allOf;

public class DigitalLetterRequestTest {
	private final String subject = "This is the subject of the letter";
	private final String body = "This is the body of the letter";
	private final String contentType = "text/plain";
	private final List<String> recipientPartyIds = List.of("6d0773d6-3e7f-4552-81bc-f0007af95adf");

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void testBean() {
		org.hamcrest.MatcherAssert.assertThat(DigitalLetterRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderPattern() {
		final var request = DigitalLetterRequest.create()
			.withSubject(subject)
			.withBody(body)
			.withContentType(contentType)
			.withRecipientPartyIds(recipientPartyIds);

		assertThat(request.getSubject()).isEqualTo(subject);
		assertThat(request.getBody()).isEqualTo(body);
		assertThat(request.getContentType()).isEqualTo(contentType);
		assertThat(request.getRecipientPartyIds()).isEqualTo(recipientPartyIds);
		assertThat(request).hasNoNullFieldsOrProperties();
	}

	@Test
	void settersAndGetters() {
		final var request = new DigitalLetterRequest();
		request.setSubject(subject);
		request.setBody(body);
		request.setContentType(contentType);
		request.setRecipientPartyIds(recipientPartyIds);

		assertThat(request.getSubject()).isEqualTo(subject);
		assertThat(request.getBody()).isEqualTo(body);
		assertThat(request.getContentType()).isEqualTo(contentType);
		assertThat(request.getRecipientPartyIds()).isEqualTo(recipientPartyIds);
		assertThat(request).hasNoNullFieldsOrProperties();
	}

	@Test
	void validateEmptyBean() {
		final var request = new DigitalLetterRequest();

		final var violations = validator.validate(request);

		assertThat(violations).hasSize(2)
			.extracting(violation -> violation.getPropertyPath().toString(), ConstraintViolation::getMessage)
			.containsExactlyInAnyOrder(
				tuple("subject", "must not be blank"),
				tuple("recipientPartyIds", "must not be empty"));
		assertThat(request).hasAllNullFieldsOrProperties();
	}

	@Test
	void validatePopulatedBean() {
		final var request = DigitalLetterRequest.create()
			.withSubject(subject)
			.withBody(body)
			.withContentType(contentType)
			.withRecipientPartyIds(recipientPartyIds);

		final var violations = validator.validate(request);

		assertThat(violations).isEmpty();
		assertThat(request).hasNoNullFieldsOrProperties();
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(DigitalLetterRequest.create()).hasAllNullFieldsOrProperties();
		assertThat(new DigitalLetterRequest()).hasAllNullFieldsOrProperties();
	}
}
