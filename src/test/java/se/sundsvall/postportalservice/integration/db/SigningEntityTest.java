package se.sundsvall.postportalservice.integration.db;

import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class SigningEntityTest {

	private static final String ID = "123e4567-e89b-12d3-a456-426614174000";
	private static final String PROVIDER_CASE_ID = "1234567890";
	private static final String PROVIDER = "comfact";
	private static final String STATUS = "INITIATED";
	private static final OffsetDateTime CREATED = OffsetDateTime.of(2024, 6, 15, 12, 0, 0, 0, UTC);
	private static final AtomicInteger SEQUENCE = new AtomicInteger();

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> CREATED.plusDays(SEQUENCE.incrementAndGet()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		org.hamcrest.MatcherAssert.assertThat(SigningEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("message", "attachment"),
			hasValidBeanEqualsExcluding("message", "attachment"),
			hasValidBeanToStringExcluding("message", "attachment")));
	}

	@Test
	void builderTest() {
		final var message = new MessageEntity();
		final var attachment = new AttachmentEntity();
		final var signingEntity = SigningEntity.create()
			.withId(ID)
			.withMessage(message)
			.withProviderCaseId(PROVIDER_CASE_ID)
			.withProvider(PROVIDER)
			.withStatus(STATUS)
			.withAttachment(attachment)
			.withCreated(CREATED);

		assertThat(signingEntity.getId()).isEqualTo(ID);
		assertThat(signingEntity.getMessage()).isEqualTo(message);
		assertThat(signingEntity.getProviderCaseId()).isEqualTo(PROVIDER_CASE_ID);
		assertThat(signingEntity.getProvider()).isEqualTo(PROVIDER);
		assertThat(signingEntity.getStatus()).isEqualTo(STATUS);
		assertThat(signingEntity.getAttachment()).isEqualTo(attachment);
		assertThat(signingEntity.getCreated()).isEqualTo(CREATED);
		assertThat(signingEntity).hasNoNullFieldsOrProperties();
	}

	@Test
	void setterAndGetterTest() {
		final var message = new MessageEntity();
		final var attachment = new AttachmentEntity();
		final var signingEntity = new SigningEntity();

		signingEntity.setId(ID);
		signingEntity.setMessage(message);
		signingEntity.setProviderCaseId(PROVIDER_CASE_ID);
		signingEntity.setProvider(PROVIDER);
		signingEntity.setStatus(STATUS);
		signingEntity.setAttachment(attachment);
		signingEntity.setCreated(CREATED);

		assertThat(signingEntity.getId()).isEqualTo(ID);
		assertThat(signingEntity.getMessage()).isEqualTo(message);
		assertThat(signingEntity.getProviderCaseId()).isEqualTo(PROVIDER_CASE_ID);
		assertThat(signingEntity.getProvider()).isEqualTo(PROVIDER);
		assertThat(signingEntity.getStatus()).isEqualTo(STATUS);
		assertThat(signingEntity.getAttachment()).isEqualTo(attachment);
		assertThat(signingEntity.getCreated()).isEqualTo(CREATED);
		assertThat(signingEntity).hasNoNullFieldsOrProperties();
	}

	@Test
	void constructorTest() {
		assertThat(new SigningEntity()).hasAllNullFieldsOrProperties();
	}
}
