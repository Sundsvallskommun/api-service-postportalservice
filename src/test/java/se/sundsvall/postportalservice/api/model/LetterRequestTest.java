package se.sundsvall.postportalservice.api.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.hamcrest.CoreMatchers.allOf;
import static se.sundsvall.postportalservice.TestDataFactory.createValidAddress;
import static se.sundsvall.postportalservice.TestDataFactory.createValidRecipient;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import org.junit.jupiter.api.Test;

class LetterRequestTest {

	private final String subject = "This is the subject of the letter";
	private final String body = "This is the body of the letter";
	private final String contentType = "text/plain";
	private final List<Recipient> recipients = List.of(createValidRecipient());
	private final List<Address> addresses = List.of(createValidAddress());

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void testBean() {
		org.hamcrest.MatcherAssert.assertThat(LetterRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderPattern() {
		final var letterRequest = LetterRequest.create()
			.withSubject(subject)
			.withBody(body)
			.withContentType(contentType)
			.withRecipients(recipients)
			.withAddresses(addresses);

		assertThat(letterRequest.getSubject()).isEqualTo(subject);
		assertThat(letterRequest.getBody()).isEqualTo(body);
		assertThat(letterRequest.getContentType()).isEqualTo(contentType);
		assertThat(letterRequest.getRecipients()).isEqualTo(recipients);
		assertThat(letterRequest.getAddresses()).isEqualTo(addresses);
		assertThat(letterRequest).hasNoNullFieldsOrProperties();
	}

	@Test
	void settersAndGetters() {
		final var letterRequest = new LetterRequest();
		letterRequest.setSubject(subject);
		letterRequest.setBody(body);
		letterRequest.setContentType(contentType);
		letterRequest.setRecipients(recipients);
		letterRequest.setAddresses(addresses);

		assertThat(letterRequest.getSubject()).isEqualTo(subject);
		assertThat(letterRequest.getBody()).isEqualTo(body);
		assertThat(letterRequest.getContentType()).isEqualTo(contentType);
		assertThat(letterRequest.getRecipients()).isEqualTo(recipients);
		assertThat(letterRequest.getAddresses()).isEqualTo(addresses);
		assertThat(letterRequest).hasNoNullFieldsOrProperties();
	}

	@Test
	void validateEmptyBean() {
		final var letterRequest = new LetterRequest();

		final var violations = validator.validate(letterRequest);

		assertThat(violations).hasSize(1)
			.extracting(violation -> violation.getPropertyPath().toString(), ConstraintViolation::getMessage)
			.containsExactlyInAnyOrder(
				tuple("subject", "must not be blank"));
		assertThat(letterRequest).hasAllNullFieldsOrProperties();
	}

	@Test
	void validatePopulatedBean() {
		final var letterRequest = LetterRequest.create()
			.withSubject(subject)
			.withBody(body)
			.withContentType(contentType)
			.withRecipients(recipients)
			.withAddresses(addresses);

		final var violations = validator.validate(letterRequest);

		assertThat(violations).isEmpty();
		assertThat(letterRequest).hasNoNullFieldsOrProperties();
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(LetterRequest.create()).hasAllNullFieldsOrProperties();
		assertThat(new LetterRequest()).hasAllNullFieldsOrProperties();
	}
}
