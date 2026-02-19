package se.sundsvall.postportalservice.api.model;

import java.util.List;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class KivraEligibilityRequestTest {

	private static final String PARTY_ID_1 = "partyId1";
	private static final String PARTY_ID_2 = "partyId2";
	private static final List<String> PARTY_IDS = List.of(PARTY_ID_1, PARTY_ID_2);

	@Test
	void testKivraEligibilityRequest() {
		org.hamcrest.MatcherAssert.assertThat(KivraEligibilityRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void gettersAndSetters() {
		final var bean = new KivraEligibilityRequest();
		bean.setPartyIds(PARTY_IDS);

		assertThat(bean.getPartyIds()).isEqualTo(PARTY_IDS);
		assertThat(bean).hasNoNullFieldsOrProperties();
	}

	@Test
	void builderPattern() {
		final var bean = KivraEligibilityRequest.create()
			.withPartyIds(PARTY_IDS);

		assertThat(bean.getPartyIds()).isEqualTo(PARTY_IDS);
		assertThat(bean).hasNoNullFieldsOrProperties();
	}

	@Test
	void constructorTest() {
		assertThat(new KivraEligibilityRequest()).hasAllNullFieldsOrProperties();
		assertThat(new KivraEligibilityRequest()).hasOnlyFields("partyIds");
	}

}
