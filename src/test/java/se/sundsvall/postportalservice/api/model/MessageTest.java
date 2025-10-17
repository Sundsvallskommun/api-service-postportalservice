package se.sundsvall.postportalservice.api.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

import java.time.LocalDateTime;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MessageTest {

	private static final String MESSAGE_ID = "messageId";
	private static final String SUBJECT = "subject";
	private static final String TYPE = "type";
	private static final LocalDateTime SENT_AT = LocalDateTime.of(2021, 1, 1, 12, 0, 0);
	private static final SigningStatus SIGNING_STATUS = SigningStatus.create();

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), LocalDateTime.class);
	}

	@Test
	void testBean() {
		org.hamcrest.MatcherAssert.assertThat(Message.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void getterAndSetterTest() {
		final var bean = new Message();
		bean.setMessageId(MESSAGE_ID);
		bean.setSentAt(SENT_AT);
		bean.setSigningStatus(SIGNING_STATUS);
		bean.setSubject(SUBJECT);
		bean.setType(TYPE);

		assertBean(bean);
	}

	@Test
	void builderPatternTest() {
		final var bean = new Message()
			.withMessageId(MESSAGE_ID)
			.withSentAt(SENT_AT)
			.withSigningStatus(SIGNING_STATUS)
			.withSubject(SUBJECT)
			.withType(TYPE);

		assertBean(bean);
	}

	private void assertBean(final Message bean) {
		assertThat(bean).hasNoNullFieldsOrProperties();
		assertThat(bean.getMessageId()).isEqualTo(MESSAGE_ID);
		assertThat(bean.getSentAt()).isEqualTo(SENT_AT);
		assertThat(bean.getSigningStatus()).isEqualTo(SIGNING_STATUS);
		assertThat(bean.getSubject()).isEqualTo(SUBJECT);
		assertThat(bean.getType()).isEqualTo(TYPE);
	}

	@Test
	void constructorTest() {
		assertThat(new Message()).hasAllNullFieldsOrPropertiesExcept("numberOfRecipients").extracting("numberOfRecipients").isEqualTo(0);
		assertThat(Message.create()).hasAllNullFieldsOrPropertiesExcept("numberOfRecipients").extracting("numberOfRecipients").isEqualTo(0);
	}

}
