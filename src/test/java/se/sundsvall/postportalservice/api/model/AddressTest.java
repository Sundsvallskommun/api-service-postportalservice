package se.sundsvall.postportalservice.api.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.hamcrest.CoreMatchers.allOf;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class AddressTest {

	private final String firstName = "John";
	private final String lastName = "Doe";
	private final String street = "Main Street 1";
	private final String apartmentNumber = "1101";
	private final String careOf = "c/o Jane Doe";
	private final String zipCode = "12345";
	private final String city = "Sundsvall";
	private final String country = "Sweden";

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void testBean() {
		org.hamcrest.MatcherAssert.assertThat(Address.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderPattern() {
		final var address = Address.create()
			.withFirstName(firstName)
			.withLastName(lastName)
			.withStreet(street)
			.withApartmentNumber(apartmentNumber)
			.withCareOf(careOf)
			.withZipCode(zipCode)
			.withCity(city)
			.withCountry(country);

		assertThat(address.getFirstName()).isEqualTo(firstName);
		assertThat(address.getLastName()).isEqualTo(lastName);
		assertThat(address.getStreet()).isEqualTo(street);
		assertThat(address.getApartmentNumber()).isEqualTo(apartmentNumber);
		assertThat(address.getCareOf()).isEqualTo(careOf);
		assertThat(address.getZipCode()).isEqualTo(zipCode);
		assertThat(address.getCity()).isEqualTo(city);
		assertThat(address.getCountry()).isEqualTo(country);
		assertThat(address).hasNoNullFieldsOrProperties();
	}

	@Test
	void settersAndGetters() {
		final var address = new Address();
		address.setFirstName(firstName);
		address.setLastName(lastName);
		address.setStreet(street);
		address.setApartmentNumber(apartmentNumber);
		address.setCareOf(careOf);
		address.setZipCode(zipCode);
		address.setCity(city);
		address.setCountry(country);

		assertThat(address.getFirstName()).isEqualTo(firstName);
		assertThat(address.getLastName()).isEqualTo(lastName);
		assertThat(address.getStreet()).isEqualTo(street);
		assertThat(address.getApartmentNumber()).isEqualTo(apartmentNumber);
		assertThat(address.getCareOf()).isEqualTo(careOf);
		assertThat(address.getZipCode()).isEqualTo(zipCode);
		assertThat(address.getCity()).isEqualTo(city);
		assertThat(address.getCountry()).isEqualTo(country);
		assertThat(address).hasNoNullFieldsOrProperties();
	}

	@Test
	void validateEmptyBean() {
		final var address = new Address();

		final var violations = validator.validate(address);

		assertThat(violations).hasSize(6)
			.extracting(violation -> violation.getPropertyPath().toString(), ConstraintViolation::getMessage)
			.containsExactlyInAnyOrder(
				tuple("firstName", "must not be blank"),
				tuple("lastName", "must not be blank"),
				tuple("street", "must not be blank"),
				tuple("zipCode", "must not be blank"),
				tuple("city", "must not be blank"),
				tuple("country", "must not be blank"));

		assertThat(address).hasAllNullFieldsOrProperties();
	}

	@Test
	void validatePopulatedBean() {
		final var address = Address.create()
			.withFirstName(firstName)
			.withLastName(lastName)
			.withStreet(street)
			.withApartmentNumber(apartmentNumber)
			.withCareOf(careOf)
			.withZipCode(zipCode)
			.withCity(city)
			.withCountry(country);

		final var violations = validator.validate(address);

		assertThat(violations).isEmpty();
		assertThat(address).hasNoNullFieldsOrProperties();
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(new Address()).hasAllNullFieldsOrProperties();
		assertThat(Address.create()).hasAllNullFieldsOrProperties();
	}
}
