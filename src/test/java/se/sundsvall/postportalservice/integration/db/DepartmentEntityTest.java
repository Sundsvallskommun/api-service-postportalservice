package se.sundsvall.postportalservice.integration.db;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

class DepartmentEntityTest {

	private static final String ID = "123e4567-e89b-12d3-a456-426614174000";
	private static final String NAME = "name";
	private static final String ORGANIZATION_NUMBER = "1234567890";
	private static final String ORGANIZATION_ID = "1234567890";
	private static final String SUPPORT_TEXT = "supportText";
	private static final String CONTACT_INFORMATION_URL = "contactInformationUrl";
	private static final String CONTACT_INFORMATION_EMAIL = "contactInformationEmail";
	private static final String CONTACT_INFORMATION_PHONE_NUMBER = "contactInformationPhoneNumber";

	@Test
	void testBean() {
		assertThat(DepartmentEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderPattern() {
		final var departmentEntity = DepartmentEntity.create()
			.withId(ID)
			.withName(NAME)
			.withOrganizationNumber(ORGANIZATION_NUMBER)
			.withOrganizationId(ORGANIZATION_ID)
			.withSupportText(SUPPORT_TEXT)
			.withContactInformationUrl(CONTACT_INFORMATION_URL)
			.withContactInformationEmail(CONTACT_INFORMATION_EMAIL)
			.withContactInformationPhoneNumber(CONTACT_INFORMATION_PHONE_NUMBER);

		assertThat(departmentEntity.getId()).isEqualTo(ID);
		assertThat(departmentEntity.getName()).isEqualTo(NAME);
		assertThat(departmentEntity.getOrganizationNumber()).isEqualTo(ORGANIZATION_NUMBER);
		assertThat(departmentEntity.getOrganizationId()).isEqualTo(ORGANIZATION_ID);
		assertThat(departmentEntity.getSupportText()).isEqualTo(SUPPORT_TEXT);
		assertThat(departmentEntity.getContactInformationUrl()).isEqualTo(CONTACT_INFORMATION_URL);
		assertThat(departmentEntity.getContactInformationEmail()).isEqualTo(CONTACT_INFORMATION_EMAIL);
		assertThat(departmentEntity.getContactInformationPhoneNumber()).isEqualTo(CONTACT_INFORMATION_PHONE_NUMBER);
		assertThat(departmentEntity).hasNoNullFieldsOrProperties();
	}

	@Test
	void testGettersAndSetters() {
		final var departmentEntity = new DepartmentEntity();
		departmentEntity.setId(ID);
		departmentEntity.setName(NAME);
		departmentEntity.setOrganizationNumber(ORGANIZATION_NUMBER);
		departmentEntity.setOrganizationId(ORGANIZATION_ID);
		departmentEntity.setSupportText(SUPPORT_TEXT);
		departmentEntity.setContactInformationUrl(CONTACT_INFORMATION_URL);
		departmentEntity.setContactInformationEmail(CONTACT_INFORMATION_EMAIL);
		departmentEntity.setContactInformationPhoneNumber(CONTACT_INFORMATION_PHONE_NUMBER);

		assertThat(departmentEntity.getId()).isEqualTo(ID);
		assertThat(departmentEntity.getName()).isEqualTo(NAME);
		assertThat(departmentEntity.getOrganizationNumber()).isEqualTo(ORGANIZATION_NUMBER);
		assertThat(departmentEntity.getOrganizationId()).isEqualTo(ORGANIZATION_ID);
		assertThat(departmentEntity.getSupportText()).isEqualTo(SUPPORT_TEXT);
		assertThat(departmentEntity.getContactInformationUrl()).isEqualTo(CONTACT_INFORMATION_URL);
		assertThat(departmentEntity.getContactInformationEmail()).isEqualTo(CONTACT_INFORMATION_EMAIL);
		assertThat(departmentEntity.getContactInformationPhoneNumber()).isEqualTo(CONTACT_INFORMATION_PHONE_NUMBER);
		assertThat(departmentEntity).hasNoNullFieldsOrProperties();
	}

	@Test
	void constructorTest() {
		final var departmentEntity = new DepartmentEntity();
		assertThat(departmentEntity).hasAllNullFieldsOrProperties();
	}
}
