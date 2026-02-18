package se.sundsvall.postportalservice.api.model;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class StatisticsTest {

	private static final String ID = "id";
	private static final String NAME = "name";
	private static final Long SNAIL_MAIL = 10L;
	private static final Long DIGITAL_MAIL = 20L;
	private static final Long SMS = 30L;
	private static final Long DIGITAL_REGISTERED_LETTER = 40L;

	@Test
	void testBean() {
		org.hamcrest.MatcherAssert.assertThat(Statistics.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderPattern() {
		final var statistics = Statistics.create()
			.withId(ID)
			.withName(NAME)
			.withSnailMail(SNAIL_MAIL)
			.withDigitalMail(DIGITAL_MAIL)
			.withSms(SMS)
			.withDigitalRegisteredLetter(DIGITAL_REGISTERED_LETTER);

		assertThat(statistics.getId()).isEqualTo(ID);
		assertThat(statistics.getName()).isEqualTo(NAME);
		assertThat(statistics.getSnailMail()).isEqualTo(SNAIL_MAIL);
		assertThat(statistics.getDigitalMail()).isEqualTo(DIGITAL_MAIL);
		assertThat(statistics.getSms()).isEqualTo(SMS);
		assertThat(statistics.getDigitalRegisteredLetter()).isEqualTo(DIGITAL_REGISTERED_LETTER);
		assertThat(statistics).hasNoNullFieldsOrProperties();
	}

	@Test
	void settersAndGetters() {
		final var statistics = new Statistics();
		statistics.setId(ID);
		statistics.setName(NAME);
		statistics.setSnailMail(SNAIL_MAIL);
		statistics.setDigitalMail(DIGITAL_MAIL);
		statistics.setSms(SMS);
		statistics.setDigitalRegisteredLetter(DIGITAL_REGISTERED_LETTER);

		assertThat(statistics.getId()).isEqualTo(ID);
		assertThat(statistics.getName()).isEqualTo(NAME);
		assertThat(statistics.getSnailMail()).isEqualTo(SNAIL_MAIL);
		assertThat(statistics.getDigitalMail()).isEqualTo(DIGITAL_MAIL);
		assertThat(statistics.getSms()).isEqualTo(SMS);
		assertThat(statistics.getDigitalRegisteredLetter()).isEqualTo(DIGITAL_REGISTERED_LETTER);
		assertThat(statistics).hasNoNullFieldsOrProperties();
	}

}
