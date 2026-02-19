package se.sundsvall.postportalservice.api.model;

import java.time.OffsetDateTime;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class SigningInformationTest {

	// SigningInformation attributes
	private static final String STATUS = "status";
	private static final OffsetDateTime SIGNED_AT = now();
	private static final String CONTENT_KEY = "contentKey";
	private static final String ORDER_REFERENCE = "orderReference";
	private static final String SIGNATURE = "signature";
	private static final String OCSP_RESPONSE = "ocspResponse";

	// SigningInformation.User attributes
	private static final String PERSONAL_IDENTITY_NUMBER = "personalIdentityNumber";
	private static final String NAME = "name";
	private static final String GIVEN_NAME = "givenName";
	private static final String SURNAME = "surname";

	// SigningInformation.Device attributes
	private static final String IP_ADDRESS = "ipAddress";

	// SigningInformation.StepUp attributes
	private static final Boolean MRTD = true;

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testSigningInformation() {
		org.hamcrest.MatcherAssert.assertThat(SigningInformation.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testSigningInformationUser() {
		org.hamcrest.MatcherAssert.assertThat(SigningInformation.User.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testSigningInformationDevice() {
		org.hamcrest.MatcherAssert.assertThat(SigningInformation.Device.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testSigningInformationStepUp() {
		org.hamcrest.MatcherAssert.assertThat(SigningInformation.StepUp.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void signingInformation_gettersAndSetters() {
		final var bean = new SigningInformation();
		bean.setStatus(STATUS);
		bean.setSignedAt(SIGNED_AT);
		bean.setContentKey(CONTENT_KEY);
		bean.setOrderReference(ORDER_REFERENCE);
		bean.setSignature(SIGNATURE);
		bean.setOcspResponse(OCSP_RESPONSE);
		bean.setUser(new SigningInformation.User());
		bean.setDevice(new SigningInformation.Device());
		bean.setStepUp(new SigningInformation.StepUp());

		assertThat(bean.getStatus()).isEqualTo(STATUS);
		assertThat(bean.getSignedAt()).isEqualTo(SIGNED_AT);
		assertThat(bean.getContentKey()).isEqualTo(CONTENT_KEY);
		assertThat(bean.getOrderReference()).isEqualTo(ORDER_REFERENCE);
		assertThat(bean.getSignature()).isEqualTo(SIGNATURE);
		assertThat(bean.getOcspResponse()).isEqualTo(OCSP_RESPONSE);
		assertThat(bean.getUser()).isEqualTo(new SigningInformation.User());
		assertThat(bean.getDevice()).isEqualTo(new SigningInformation.Device());
		assertThat(bean.getStepUp()).isEqualTo(new SigningInformation.StepUp());
		assertThat(bean).hasNoNullFieldsOrProperties();
	}

	@Test
	void signingInformationUser_gettersAndSetters() {
		final var bean = new SigningInformation.User();
		bean.setPersonalIdentityNumber(PERSONAL_IDENTITY_NUMBER);
		bean.setName(NAME);
		bean.setGivenName(GIVEN_NAME);
		bean.setSurname(SURNAME);

		assertThat(bean.getPersonalIdentityNumber()).isEqualTo(PERSONAL_IDENTITY_NUMBER);
		assertThat(bean.getName()).isEqualTo(NAME);
		assertThat(bean.getGivenName()).isEqualTo(GIVEN_NAME);
		assertThat(bean.getSurname()).isEqualTo(SURNAME);
		assertThat(bean).hasNoNullFieldsOrProperties();
	}

	@Test
	void signingInformationDevice_gettersAndSetters() {
		final var bean = new SigningInformation.Device();
		bean.setIpAddress(IP_ADDRESS);

		assertThat(bean.getIpAddress()).isEqualTo(IP_ADDRESS);
		assertThat(bean).hasNoNullFieldsOrProperties();
	}

	@Test
	void signingInformationStepUp_gettersAndSetters() {
		final var bean = new SigningInformation.StepUp();
		bean.setMrtd(MRTD);

		assertThat(bean.getMrtd()).isEqualTo(MRTD);
		assertThat(bean).hasNoNullFieldsOrProperties();
	}

	@Test
	void signingInformation_builderPattern() {
		final var bean = SigningInformation.create()
			.withStatus(STATUS)
			.withSignedAt(SIGNED_AT)
			.withContentKey(CONTENT_KEY)
			.withOrderReference(ORDER_REFERENCE)
			.withSignature(SIGNATURE)
			.withOcspResponse(OCSP_RESPONSE)
			.withUser(new SigningInformation.User()
				.withPersonalIdentityNumber(PERSONAL_IDENTITY_NUMBER)
				.withName(NAME)
				.withGivenName(GIVEN_NAME)
				.withSurname(SURNAME))
			.withDevice(new SigningInformation.Device()
				.withIpAddress(IP_ADDRESS))
			.withStepUp(new SigningInformation.StepUp()
				.withMrtd(MRTD));

		assertThat(bean.getStatus()).isEqualTo(STATUS);
		assertThat(bean.getSignedAt()).isEqualTo(SIGNED_AT);
		assertThat(bean.getContentKey()).isEqualTo(CONTENT_KEY);
		assertThat(bean.getOrderReference()).isEqualTo(ORDER_REFERENCE);
		assertThat(bean.getSignature()).isEqualTo(SIGNATURE);
		assertThat(bean.getOcspResponse()).isEqualTo(OCSP_RESPONSE);
		assertThat(bean.getUser()).isNotNull().satisfies(user -> {
			assertThat(user.getPersonalIdentityNumber()).isEqualTo(PERSONAL_IDENTITY_NUMBER);
			assertThat(user.getName()).isEqualTo(NAME);
			assertThat(user.getGivenName()).isEqualTo(GIVEN_NAME);
			assertThat(user.getSurname()).isEqualTo(SURNAME);
		});
		assertThat(bean.getDevice()).isNotNull().satisfies(device -> assertThat(device.getIpAddress()).isEqualTo(IP_ADDRESS));
		assertThat(bean.getStepUp()).isNotNull().satisfies(stepUp -> assertThat(stepUp.getMrtd()).isEqualTo(MRTD));

		assertThat(bean).hasNoNullFieldsOrProperties();
	}

	@Test
	void signingInformationUser_builderPattern() {
		final var bean = SigningInformation.User.create()
			.withPersonalIdentityNumber(PERSONAL_IDENTITY_NUMBER)
			.withName(NAME)
			.withGivenName(GIVEN_NAME)
			.withSurname(SURNAME);

		assertThat(bean.getPersonalIdentityNumber()).isEqualTo(PERSONAL_IDENTITY_NUMBER);
		assertThat(bean.getName()).isEqualTo(NAME);
		assertThat(bean.getGivenName()).isEqualTo(GIVEN_NAME);
		assertThat(bean.getSurname()).isEqualTo(SURNAME);
		assertThat(bean).hasNoNullFieldsOrProperties();
	}

	@Test
	void signingInformationDevice_builderPattern() {
		final var bean = SigningInformation.Device.create()
			.withIpAddress(IP_ADDRESS);

		assertThat(bean.getIpAddress()).isEqualTo(IP_ADDRESS);
		assertThat(bean).hasNoNullFieldsOrProperties();
	}

	@Test
	void signingInformationStepUp_builderPattern() {
		final var bean = SigningInformation.StepUp.create()
			.withMrtd(MRTD);

		assertThat(bean.getMrtd()).isEqualTo(MRTD);
		assertThat(bean).hasNoNullFieldsOrProperties();
	}

	@Test
	void signingInformation_constructorTest() {
		assertThat(new SigningInformation()).hasAllNullFieldsOrProperties();
		assertThat(new SigningInformation()).hasOnlyFields("status", "signedAt", "contentKey", "orderReference", "signature", "ocspResponse", "user", "device", "stepUp");
	}

	@Test
	void signingInformationUser_constructorTest() {
		assertThat(new SigningInformation.User()).hasAllNullFieldsOrProperties();
		assertThat(new SigningInformation.User()).hasOnlyFields("personalIdentityNumber", "name", "givenName", "surname");
	}

	@Test
	void signingInformationDevice_constructorTest() {
		assertThat(new SigningInformation.Device()).hasAllNullFieldsOrProperties();
		assertThat(new SigningInformation.Device()).hasOnlyFields("ipAddress");
	}

	@Test
	void signingInformationStepUp_constructorTest() {
		assertThat(new SigningInformation.StepUp()).hasAllNullFieldsOrProperties();
		assertThat(new SigningInformation.StepUp()).hasOnlyFields("mrtd");
	}

}
