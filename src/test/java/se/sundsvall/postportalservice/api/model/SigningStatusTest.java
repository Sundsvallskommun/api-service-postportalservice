package se.sundsvall.postportalservice.api.model;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class SigningStatusTest {

	private static final String LETTER_STATE = "letterState";
	private static final String SIGNING_PROCESS_STATE = "signingProcessState";

	@Test
	void testBean() {
		org.hamcrest.MatcherAssert.assertThat(SigningStatus.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void getterAndSetterTest() {
		final var bean = new SigningStatus();
		bean.setLetterState(LETTER_STATE);
		bean.setSigningProcessState(SIGNING_PROCESS_STATE);

		assertBean(bean);
	}

	@Test
	void builderPatternTest() {
		final var bean = new SigningStatus()
			.withLetterState(LETTER_STATE)
			.withSigningProcessState(SIGNING_PROCESS_STATE);

		assertBean(bean);
	}

	private void assertBean(final SigningStatus bean) {
		assertThat(bean).hasNoNullFieldsOrProperties();
		assertThat(bean.getLetterState()).isEqualTo(LETTER_STATE);
		assertThat(bean.getSigningProcessState()).isEqualTo(SIGNING_PROCESS_STATE);
	}

	@Test
	void constructorTest() {
		assertThat(new SigningStatus()).hasOnlyFields("letterState", "signingProcessState");
		assertThat(new SigningStatus()).hasAllNullFieldsOrProperties();
		assertThat(SigningStatus.create()).hasAllNullFieldsOrProperties();
	}

}
