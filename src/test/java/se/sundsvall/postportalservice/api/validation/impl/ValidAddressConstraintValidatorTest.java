package se.sundsvall.postportalservice.api.validation.impl;

import org.junit.jupiter.api.Test;
import se.sundsvall.postportalservice.api.model.Address;

import static org.assertj.core.api.Assertions.assertThat;

class ValidAddressConstraintValidatorTest {

	private final ValidAddressConstraintValidator validator = new ValidAddressConstraintValidator();

	@Test
	void nullAddressIsValid() {
		assertThat(validator.isValid(null, null)).isTrue();
	}

	@Test
	void onlyFirstNameIsInvalid() {
		var address = Address.create()
			.withFirstName("John");
		assertThat(validator.isValid(address, null)).isFalse();
	}

	@Test
	void onlyLastNameIsInvalid() {
		var address = Address.create()
			.withLastName("Doe");
		assertThat(validator.isValid(address, null)).isFalse();
	}

	@Test
	void firstAndLastNameIsValid() {
		var address = Address.create()
			.withFirstName("John")
			.withLastName("Doe");
		assertThat(validator.isValid(address, null)).isTrue();
	}

	@Test
	void onlyOrganizationNameIsValid() {
		var address = Address.create()
			.withOrganizationName("Acme AB");
		assertThat(validator.isValid(address, null)).isTrue();
	}

	@Test
	void allThreeNamesIsValid() {
		var address = Address.create()
			.withFirstName("John")
			.withLastName("Doe")
			.withOrganizationName("Acme AB");
		assertThat(validator.isValid(address, null)).isTrue();
	}

	@Test
	void noNamesIsInvalid() {
		var address = Address.create();
		assertThat(validator.isValid(address, null)).isFalse();
	}

	@Test
	void blankNamesIsInvalid() {
		var address = Address.create()
			.withFirstName("  ")
			.withLastName("  ")
			.withOrganizationName(" ");
		assertThat(validator.isValid(address, null)).isFalse();
	}
}
