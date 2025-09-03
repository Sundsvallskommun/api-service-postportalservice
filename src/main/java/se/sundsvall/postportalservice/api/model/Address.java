package se.sundsvall.postportalservice.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;

@Schema(description = "Address model")
public class Address {

	@Schema(description = "First name of the recipient", example = "John")
	@NotBlank
	private String firstName;

	@Schema(description = "Last name of the recipient", example = "Doe")
	@NotBlank
	private String lastName;

	@Schema(description = "Street address", example = "Main Street 1")
	@NotBlank
	private String street;

	@Schema(description = "Apartment number", example = "1101")
	private String apartmentNumber;

	@Schema(description = "Care of", example = "c/o Jane Doe")
	private String careOf;

	@Schema(description = "Zip code", example = "12345")
	@NotBlank
	private String zipCode;

	@Schema(description = "City", example = "Sundsvall")
	@NotBlank
	private String city;

	@Schema(description = "Country", example = "Sweden")
	@NotBlank
	private String country;

	public static Address create() {
		return new Address();
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public Address withFirstName(String firstName) {
		this.firstName = firstName;
		return this;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Address withLastName(String lastName) {
		this.lastName = lastName;
		return this;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public Address withStreet(String street) {
		this.street = street;
		return this;
	}

	public String getApartmentNumber() {
		return apartmentNumber;
	}

	public void setApartmentNumber(String apartmentNumber) {
		this.apartmentNumber = apartmentNumber;
	}

	public Address withApartmentNumber(String apartmentNumber) {
		this.apartmentNumber = apartmentNumber;
		return this;
	}

	public String getCareOf() {
		return careOf;
	}

	public void setCareOf(String careOf) {
		this.careOf = careOf;
	}

	public Address withCareOf(String careOf) {
		this.careOf = careOf;
		return this;
	}

	public String getZipCode() {
		return zipCode;
	}

	public void setZipCode(String zipCode) {
		this.zipCode = zipCode;
	}

	public Address withZipCode(String zipCode) {
		this.zipCode = zipCode;
		return this;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public Address withCity(String city) {
		this.city = city;
		return this;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public Address withCountry(String country) {
		this.country = country;
		return this;
	}

	@Override
	public String toString() {
		return "Address{" +
			"firstName='" + firstName + '\'' +
			", lastName='" + lastName + '\'' +
			", street='" + street + '\'' +
			", apartmentNumber='" + apartmentNumber + '\'' +
			", careOf='" + careOf + '\'' +
			", zipCode='" + zipCode + '\'' +
			", city='" + city + '\'' +
			", country='" + country + '\'' +
			'}';
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		Address address = (Address) o;
		return Objects.equals(firstName, address.firstName) && Objects.equals(lastName, address.lastName) && Objects.equals(street, address.street) && Objects.equals(apartmentNumber, address.apartmentNumber)
			&& Objects.equals(careOf, address.careOf) && Objects.equals(zipCode, address.zipCode) && Objects.equals(city, address.city) && Objects.equals(country, address.country);
	}

	@Override
	public int hashCode() {
		return Objects.hash(firstName, lastName, street, apartmentNumber, careOf, zipCode, city, country);
	}
}
