package se.sundsvall.postportalservice.api.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.OffsetDateTime;
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

class SigningEventTest {

	private static final OffsetDateTime OCCURRED_AT = OffsetDateTime.of(2026, 6, 15, 12, 0, 0, 0, UTC);
	private static final AtomicInteger SEQUENCE = new AtomicInteger();

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> OCCURRED_AT.plusDays(SEQUENCE.incrementAndGet()), OffsetDateTime.class);
		registerValueGenerator(() -> EventSignatory.create().withPartyId(String.valueOf(SEQUENCE.incrementAndGet())), EventSignatory.class);
		registerValueGenerator(() -> SignedDocument.create().withContent(String.valueOf(SEQUENCE.incrementAndGet())), SignedDocument.class);
	}

	@Test
	void testBean() {
		org.hamcrest.MatcherAssert.assertThat(SigningEvent.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderPattern() {
		final var event = SigningEvent.create()
			.withCustomerReference("msg-1")
			.withProviderCaseId("1234567890")
			.withProvider("comfact")
			.withEventType("CASE_COMPLETED")
			.withStatus("SIGNED")
			.withSignatory(EventSignatory.create().withPartyId("p1"))
			.withSignedDocument(SignedDocument.create().withContent("c2lnbmVk"))
			.withOccurredAt(OCCURRED_AT);

		assertThat(event.getCustomerReference()).isEqualTo("msg-1");
		assertThat(event.getProviderCaseId()).isEqualTo("1234567890");
		assertThat(event.getProvider()).isEqualTo("comfact");
		assertThat(event.getEventType()).isEqualTo("CASE_COMPLETED");
		assertThat(event.getStatus()).isEqualTo("SIGNED");
		assertThat(event.getSignatory()).isNotNull();
		assertThat(event.getSignedDocument()).isNotNull();
		assertThat(event.getOccurredAt()).isEqualTo(OCCURRED_AT);
		assertThat(event).hasNoNullFieldsOrProperties();
	}

	@Test
	void validateEmptyBean() {
		assertThat(validator.validate(new SigningEvent()))
			.extracting(violation -> violation.getPropertyPath().toString(), ConstraintViolation::getMessage)
			.containsExactlyInAnyOrder(
				tuple("providerCaseId", "must not be blank"),
				tuple("eventType", "The provided event type is not a known signing event type."),
				tuple("status", "The provided status is not a known signing status."));
	}

	@Test
	void validatePopulatedBean() {
		final var event = SigningEvent.create()
			.withProviderCaseId("1234567890")
			.withEventType("CASE_COMPLETED")
			.withStatus("SIGNED");

		assertThat(validator.validate(event)).isEmpty();
	}
}
