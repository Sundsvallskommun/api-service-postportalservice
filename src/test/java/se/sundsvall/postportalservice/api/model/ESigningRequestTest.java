package se.sundsvall.postportalservice.api.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.CoreMatchers.allOf;

class ESigningRequestTest {

	private static final OffsetDateTime EXPIRES = OffsetDateTime.of(2026, 12, 31, 23, 59, 59, 0, UTC);
	private static final AtomicInteger SEQUENCE = new AtomicInteger();

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> EXPIRES.plusDays(SEQUENCE.incrementAndGet()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		org.hamcrest.MatcherAssert.assertThat(ESigningRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderPattern() {
		final var signatories = List.of(ESigningSignatory.create().withPartyId("6d0773d6-3e7f-4552-81bc-f0007af95adf").withName("John Doe").withEmail("john.doe@sundsvall.se"));
		final var request = ESigningRequest.create()
			.withSubject("Please sign")
			.withBody("Dear John Doe, please sign the attached document.")
			.withLanguage("sv-SE")
			.withExpires(EXPIRES)
			.withSignatories(signatories);

		assertThat(request.getSubject()).isEqualTo("Please sign");
		assertThat(request.getBody()).isEqualTo("Dear John Doe, please sign the attached document.");
		assertThat(request.getLanguage()).isEqualTo("sv-SE");
		assertThat(request.getExpires()).isEqualTo(EXPIRES);
		assertThat(request.getSignatories()).isEqualTo(signatories);
		assertThat(request).hasNoNullFieldsOrProperties();
	}

	@Test
	void validateEmptyBean() {
		final var violations = validator.validate(new ESigningRequest());

		assertThat(violations)
			.extracting(violation -> violation.getPropertyPath().toString(), ConstraintViolation::getMessage)
			.containsExactlyInAnyOrder(
				tuple("subject", "must not be blank"),
				tuple("body", "must not be blank"),
				tuple("signatories", "must not be empty"));
	}

	@Test
	void validatePopulatedBean() {
		final var request = ESigningRequest.create()
			.withSubject("Please sign")
			.withBody("Please sign the document")
			.withSignatories(List.of(ESigningSignatory.create().withPartyId("6d0773d6-3e7f-4552-81bc-f0007af95adf").withName("John Doe").withEmail("john.doe@sundsvall.se")));

		assertThat(validator.validate(request)).isEmpty();
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(ESigningRequest.create()).hasAllNullFieldsOrProperties();
		assertThat(new ESigningRequest()).hasAllNullFieldsOrProperties();
	}
}
