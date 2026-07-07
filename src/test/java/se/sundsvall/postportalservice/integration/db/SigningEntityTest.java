package se.sundsvall.postportalservice.integration.db;

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
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class SigningEntityTest {

	private static final String ID = "123e4567-e89b-12d3-a456-426614174000";
	private static final String MESSAGE_ID = "b2cd4957-228f-46f0-a263-d4eae2eb5f52";
	private static final String PROVIDER_CASE_ID = "1234567890";
	private static final String PROVIDER = "comfact";
	private static final String STATUS = "INITIERAT";
	private static final String ATTACHMENT_ID = "5ab7aa30-b7fc-404a-89a3-f30fa5667979";
	private static final OffsetDateTime CREATED = OffsetDateTime.of(2024, 6, 15, 12, 0, 0, 0, UTC);
	private static final AtomicInteger SEQUENCE = new AtomicInteger();

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> CREATED.plusDays(SEQUENCE.incrementAndGet()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(SigningEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderTest() {
		final var signingEntity = SigningEntity.create()
			.withId(ID)
			.withMessageId(MESSAGE_ID)
			.withProviderCaseId(PROVIDER_CASE_ID)
			.withProvider(PROVIDER)
			.withStatus(STATUS)
			.withAttachmentId(ATTACHMENT_ID)
			.withCreated(CREATED);

		assertThat(signingEntity.getId()).isEqualTo(ID);
		assertThat(signingEntity.getMessageId()).isEqualTo(MESSAGE_ID);
		assertThat(signingEntity.getProviderCaseId()).isEqualTo(PROVIDER_CASE_ID);
		assertThat(signingEntity.getProvider()).isEqualTo(PROVIDER);
		assertThat(signingEntity.getStatus()).isEqualTo(STATUS);
		assertThat(signingEntity.getAttachmentId()).isEqualTo(ATTACHMENT_ID);
		assertThat(signingEntity.getCreated()).isEqualTo(CREATED);
		assertThat(signingEntity).hasNoNullFieldsOrProperties();
	}

	@Test
	void setterAndGetterTest() {
		final var signingEntity = new SigningEntity();

		signingEntity.setId(ID);
		signingEntity.setMessageId(MESSAGE_ID);
		signingEntity.setProviderCaseId(PROVIDER_CASE_ID);
		signingEntity.setProvider(PROVIDER);
		signingEntity.setStatus(STATUS);
		signingEntity.setAttachmentId(ATTACHMENT_ID);
		signingEntity.setCreated(CREATED);

		assertThat(signingEntity.getId()).isEqualTo(ID);
		assertThat(signingEntity.getMessageId()).isEqualTo(MESSAGE_ID);
		assertThat(signingEntity.getProviderCaseId()).isEqualTo(PROVIDER_CASE_ID);
		assertThat(signingEntity.getProvider()).isEqualTo(PROVIDER);
		assertThat(signingEntity.getStatus()).isEqualTo(STATUS);
		assertThat(signingEntity.getAttachmentId()).isEqualTo(ATTACHMENT_ID);
		assertThat(signingEntity.getCreated()).isEqualTo(CREATED);
		assertThat(signingEntity).hasNoNullFieldsOrProperties();
	}

	@Test
	void constructorTest() {
		assertThat(new SigningEntity()).hasAllNullFieldsOrProperties();
	}
}
