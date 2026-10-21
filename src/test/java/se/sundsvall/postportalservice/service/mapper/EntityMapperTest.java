package se.sundsvall.postportalservice.service.mapper;

import generated.se.sundsvall.citizen.CitizenAddress;
import generated.se.sundsvall.citizen.CitizenExtended;
import generated.se.sundsvall.legalentity.LEPostAddress;
import generated.se.sundsvall.legalentity.LegalEntity2;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.postportalservice.api.model.Address;
import se.sundsvall.postportalservice.api.model.Recipient;
import se.sundsvall.postportalservice.api.model.SmsRecipient;
import se.sundsvall.postportalservice.integration.db.converter.MessageType;
import se.sundsvall.postportalservice.integration.db.converter.PartyType;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.postportalservice.Constants.PENDING;
import static se.sundsvall.postportalservice.Constants.UNDELIVERABLE;
import static se.sundsvall.postportalservice.TestDataFactory.MOBILE_NUMBER;
import static se.sundsvall.postportalservice.integration.db.converter.MessageType.LETTER;

@ExtendWith(MockitoExtension.class)
class EntityMapperTest {

	@InjectMocks
	private EntityMapper entityMapper;

	@Test
	void toRecipientEntity_smsRecipient() {
		final var smsRecipient = new SmsRecipient()
			.withPartyId(UUID.randomUUID().toString())
			.withPhoneNumber(MOBILE_NUMBER);

		final var result = entityMapper.toRecipientEntity(smsRecipient);

		assertThat(result).isNotNull();
		assertThat(result.getPartyId()).isEqualTo(smsRecipient.getPartyId());
		assertThat(result.getPhoneNumber()).isEqualTo(smsRecipient.getPhoneNumber());
		assertThat(result.getMessageType()).isEqualTo(MessageType.SMS);
		assertThat(result.getStatus()).isEqualTo(PENDING);
	}

	@Test
	void toRecipientEntity_recipient_digitalMail() {
		final var recipient = new Recipient()
			.withPartyId(UUID.randomUUID().toString())
			.withDeliveryMethod("DIGITAL_MAIL");

		final var result = entityMapper.toRecipientEntity(recipient, PartyType.PRIVATE);

		assertThat(result).isNotNull();
		assertThat(result.getPartyId()).isEqualTo(recipient.getPartyId());
		assertThat(result.getMessageType()).isEqualTo(MessageType.DIGITAL_MAIL);
		assertThat(result.getStatus()).isEqualTo(PENDING);
		assertThat(result.getFirstName()).isNull();
		assertThat(result.getLastName()).isNull();
		assertThat(result.getStreetAddress()).isNull();
		assertThat(result.getApartmentNumber()).isNull();
		assertThat(result.getCareOf()).isNull();
		assertThat(result.getZipCode()).isNull();
		assertThat(result.getCity()).isNull();
		assertThat(result.getCountry()).isNull();
	}

	@Test
	void toRecipientEntity_recipient_snailMail() {
		final var address = new Address()
			.withFirstName("First")
			.withLastName("Last")
			.withStreet("Street 1")
			.withApartmentNumber("2A")
			.withCareOf("c/o Someone")
			.withZipCode("12345")
			.withCity("City")
			.withCountry("Country");
		final var recipient = new Recipient()
			.withPartyId(UUID.randomUUID().toString())
			.withAddress(address)
			.withDeliveryMethod("SNAIL_MAIL");

		final var result = entityMapper.toRecipientEntity(recipient, PartyType.PRIVATE);

		assertThat(result).isNotNull();
		assertThat(result.getPartyId()).isEqualTo(recipient.getPartyId());
		assertThat(result.getMessageType()).isEqualTo(MessageType.SNAIL_MAIL);
		assertThat(result.getStatus()).isEqualTo(PENDING);
		assertThat(result.getFirstName()).isEqualTo(address.getFirstName());
		assertThat(result.getLastName()).isEqualTo(address.getLastName());
		assertThat(result.getStreetAddress()).isEqualTo(address.getStreet());
		assertThat(result.getApartmentNumber()).isEqualTo(address.getApartmentNumber());
		assertThat(result.getCareOf()).isEqualTo(address.getCareOf());
		assertThat(result.getZipCode()).isEqualTo(address.getZipCode());
		assertThat(result.getCity()).isEqualTo(address.getCity());
		assertThat(result.getCountry()).isEqualTo(address.getCountry());
	}

	@Test
	void toRecipientEntity_recipient_snailMail_allowsOmittedCountry() {
		final var address = new Address()
			.withFirstName("First")
			.withLastName("Last")
			.withStreet("Street 1")
			.withZipCode("12345")
			.withCity("City");
		final var recipient = new Recipient()
			.withPartyId(UUID.randomUUID().toString())
			.withAddress(address)
			.withDeliveryMethod("SNAIL_MAIL");

		final var result = entityMapper.toRecipientEntity(recipient, PartyType.PRIVATE);

		assertThat(result).isNotNull();
		assertThat(result.getCountry()).isNull();
	}

	@Test
	void toIneligibleMinorRecipientEntity() {
		final var partyId = UUID.fromString(UUID.randomUUID().toString());
		final var citizenExtended = new CitizenExtended()
			.personId(partyId);

		final var result = entityMapper.toIneligibleMinorRecipientEntity(citizenExtended);

		assertThat(result).isNotNull();
		assertThat(result.getPartyId()).isEqualTo(partyId.toString());
		assertThat(result.getMessageType()).isEqualTo(MessageType.LETTER);
		assertThat(result.getStatus()).isEqualTo("INELIGIBLE_MINOR");
		assertThat(result.getFirstName()).isNull();
		assertThat(result.getLastName()).isNull();
	}

	@Test
	void toIneligibleMinorRecipientEntity_whenCitizenIsNull() {
		final var result = entityMapper.toIneligibleMinorRecipientEntity(null);

		assertThat(result).isNull();
	}

	@Test
	void toRecipientEntity_fromAddress() {
		final var address = new Address()
			.withFirstName("john")
			.withLastName("doe")
			.withStreet("Main street 1")
			.withApartmentNumber("3B")
			.withCareOf("c/o Someone")
			.withZipCode("54321")
			.withCity("Town")
			.withCountry("Countryland");

		final var result = entityMapper.toRecipientEntity(address);

		assertThat(result).isNotNull();
		assertThat(result.getFirstName()).isEqualTo(address.getFirstName());
		assertThat(result.getLastName()).isEqualTo(address.getLastName());
		assertThat(result.getStreetAddress()).isEqualTo(address.getStreet());
		assertThat(result.getApartmentNumber()).isEqualTo(address.getApartmentNumber());
		assertThat(result.getCareOf()).isEqualTo(address.getCareOf());
		assertThat(result.getZipCode()).isEqualTo(address.getZipCode());
		assertThat(result.getCity()).isEqualTo(address.getCity());
		assertThat(result.getCountry()).isEqualTo(address.getCountry());
		assertThat(result.getMessageType()).isEqualTo(MessageType.SNAIL_MAIL);
		assertThat(result.getStatus()).isEqualTo(PENDING);
	}

	@Test
	void toRecipientEntity_fromAddress_allowsOmittedCountry() {
		final var address = new Address()
			.withFirstName("john")
			.withLastName("doe")
			.withStreet("Main street 1")
			.withZipCode("54321")
			.withCity("Town");

		final var result = entityMapper.toRecipientEntity(address);

		assertThat(result).isNotNull();
		assertThat(result.getCountry()).isNull();
	}

	@Test
	void toDigitalMailRecipientEntity() {
		final var citizenExtended = new CitizenExtended()
			.givenname("John")
			.lastname("Doe");
		final var partyId = UUID.randomUUID().toString();

		final var result = entityMapper.toDigitalMailRecipientEntity(partyId, citizenExtended);

		assertThat(result).isNotNull();
		assertThat(result.getPartyId()).isEqualTo(partyId);
		assertThat(result.getMessageType()).isEqualTo(MessageType.DIGITAL_MAIL);
		assertThat(result.getStatus()).isEqualTo(PENDING);
		assertThat(result.getFirstName()).isEqualTo("John");
		assertThat(result.getLastName()).isEqualTo("Doe");
	}

	@Test
	void toSnailMailRecipientEntity() {
		final var citizenExtended = new CitizenExtended()
			.addresses(List.of(new CitizenAddress()
				.addressType("POPULATION_REGISTRATION_ADDRESS")
				.status("ACTIVE")
				.appartmentNumber("1101")
				.address("Testgatan 1")
				.postalCode("12345")
				.city("Teststaden")
				.country("Testland")
				.co("care of")))
			.givenname("John")
			.lastname("Doe");

		final var result = entityMapper.toSnailMailRecipientEntity(citizenExtended);

		assertThat(result).isNotNull();
		assertThat(result.getFirstName()).isEqualTo("John");
		assertThat(result.getLastName()).isEqualTo("Doe");
		assertThat(result.getStreetAddress()).isEqualTo("Testgatan 1");
		assertThat(result.getApartmentNumber()).isEqualTo("1101");
		assertThat(result.getCareOf()).isEqualTo("care of");
		assertThat(result.getZipCode()).isEqualTo("12345");
		assertThat(result.getCity()).isEqualTo("Teststaden");
		assertThat(result.getCountry()).isEqualTo("Testland");
		assertThat(result.getMessageType()).isEqualTo(MessageType.SNAIL_MAIL);
		assertThat(result.getStatus()).isEqualTo(PENDING);
	}

	@Test
	void toSnailMailRecipientEntity_noValidAddress() {
		final var citizenExtended = new CitizenExtended()
			.addresses(List.of(new CitizenAddress()
				.addressType("INVALID_ADDRESS")
				.status("ACTIVE")
				.appartmentNumber("1101")
				.address("Testgatan 1")
				.postalCode("12345")
				.city("Teststaden")
				.country("Testland")
				.co("care of")))
			.givenname("John")
			.lastname("Doe");

		final var result = entityMapper.toSnailMailRecipientEntity(citizenExtended);

		assertThat(result).isNull();
	}

	@Test
	void toUndeliverableRecipientEntity() {
		final var partyId = UUID.randomUUID();
		final var citizenExtended = new CitizenExtended()
			.personId(partyId);

		final var result = entityMapper.toUndeliverableRecipientEntity(citizenExtended);

		assertThat(result).isNotNull();
		assertThat(result.getPartyId()).isEqualTo(partyId.toString());
		assertThat(result.getMessageType()).isEqualTo(LETTER);
		assertThat(result.getStatus()).isEqualTo(UNDELIVERABLE);
	}

	@Test
	void toRecipientEntity_recipient_withPartyType_enterprise() {
		final var recipient = new Recipient()
			.withPartyId(UUID.randomUUID().toString())
			.withDeliveryMethod("DIGITAL_MAIL");

		final var result = entityMapper.toRecipientEntity(recipient, PartyType.ENTERPRISE);

		assertThat(result).isNotNull();
		assertThat(result.getPartyType()).isEqualTo(PartyType.ENTERPRISE);
		assertThat(result.getMessageType()).isEqualTo(MessageType.DIGITAL_MAIL);
	}

	@Test
	void toRecipientEntity_recipient_nullReturnsNull() {
		assertThat(entityMapper.toRecipientEntity(null, PartyType.ENTERPRISE)).isNull();
	}

	@Test
	void toEnterpriseDigitalMailRecipientEntity() {
		final var partyId = UUID.randomUUID().toString();

		final var result = entityMapper.toEnterpriseDigitalMailRecipientEntity(partyId);

		assertThat(result).isNotNull();
		assertThat(result.getPartyId()).isEqualTo(partyId);
		assertThat(result.getPartyType()).isEqualTo(PartyType.ENTERPRISE);
		assertThat(result.getMessageType()).isEqualTo(MessageType.DIGITAL_MAIL);
		assertThat(result.getStatus()).isEqualTo(PENDING);
	}

	@Test
	void toEnterpriseDigitalMailRecipientEntity_nullPartyId() {
		assertThat(entityMapper.toEnterpriseDigitalMailRecipientEntity(null)).isNull();
	}

	@Test
	void toEnterpriseUndeliverableRecipientEntity() {
		final var partyId = UUID.randomUUID().toString();

		final var result = entityMapper.toEnterpriseUndeliverableRecipientEntity(partyId, "No mailbox");

		assertThat(result).isNotNull();
		assertThat(result.getPartyId()).isEqualTo(partyId);
		assertThat(result.getPartyType()).isEqualTo(PartyType.ENTERPRISE);
		assertThat(result.getMessageType()).isEqualTo(LETTER);
		assertThat(result.getStatus()).isEqualTo(UNDELIVERABLE);
		assertThat(result.getStatusDetail()).isEqualTo("No mailbox");
	}

	@Test
	void toEnterpriseUndeliverableRecipientEntity_nullPartyId() {
		assertThat(entityMapper.toEnterpriseUndeliverableRecipientEntity(null, "reason")).isNull();
	}

	@Test
	void toEnterpriseSnailMailRecipientEntity_happyPath() {
		final var partyId = UUID.randomUUID().toString();
		final var legalEntity = new LegalEntity2()
			.name("Acme AB")
			.postAddress(new LEPostAddress()
				.coAdress("c/o CEO")
				.country("Sweden")
				.postalCode("12345")
				.city("Sundsvall")
				.address1("Ankeborgsvägen 12B"));

		final var result = entityMapper.toEnterpriseSnailMailRecipientEntity(partyId, legalEntity);

		assertThat(result).isNotNull();
		assertThat(result.getPartyId()).isEqualTo(partyId);
		assertThat(result.getPartyType()).isEqualTo(PartyType.ENTERPRISE);
		assertThat(result.getMessageType()).isEqualTo(MessageType.SNAIL_MAIL);
		assertThat(result.getFirstName()).isNull();
		assertThat(result.getLastName()).isNull();
		assertThat(result.getOrganizationName()).isEqualTo("Acme AB");
		assertThat(result.getStreetAddress()).isEqualTo("Ankeborgsvägen 12B");
		assertThat(result.getCareOf()).isEqualTo("c/o CEO");
		assertThat(result.getZipCode()).isEqualTo("12345");
		assertThat(result.getCity()).isEqualTo("Sundsvall");
		assertThat(result.getCountry()).isEqualTo("Sweden");
		assertThat(result.getStatus()).isEqualTo(PENDING);
	}

	@Test
	void toEnterpriseSnailMailRecipientEntity_nullPartyId() {
		assertThat(entityMapper.toEnterpriseSnailMailRecipientEntity(null, new LegalEntity2())).isNull();
	}

	@Test
	void toEnterpriseSnailMailRecipientEntity_nullLegalEntity() {
		assertThat(entityMapper.toEnterpriseSnailMailRecipientEntity("partyId", null)).isNull();
	}

	@Test
	void toEnterpriseSnailMailRecipientEntity_missingPostAddress_returnsNull() {
		final var partyId = UUID.randomUUID().toString();
		final var legalEntity = new LegalEntity2().name("Acme");

		final var result = entityMapper.toEnterpriseSnailMailRecipientEntity(partyId, legalEntity);

		assertThat(result).isNull();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("streetAddressProvider")
	void toEnterpriseSnailMailRecipientEntity_streetAddress(String testName, String address1, String address2, String expectedStreetAddress) {
		final var legalEntity = new LegalEntity2()
			.name("Acme")
			.postAddress(new LEPostAddress()
				.address1(address1)
				.address2(address2)
				.postalCode("85222")
				.city("Sundsvall"));

		final var result = entityMapper.toEnterpriseSnailMailRecipientEntity(UUID.randomUUID().toString(), legalEntity);

		assertThat(result).isNotNull();
		assertThat(result.getStreetAddress()).isEqualTo(expectedStreetAddress);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("incompletePostAddressProvider")
	void toEnterpriseSnailMailRecipientEntity_incompletePostAddress_returnsNull(String testName, LEPostAddress postAddress) {
		final var legalEntity = new LegalEntity2()
			.name("Acme")
			.postAddress(postAddress);

		final var result = entityMapper.toEnterpriseSnailMailRecipientEntity(UUID.randomUUID().toString(), legalEntity);

		assertThat(result).isNull();
	}

	public static Stream<Arguments> streetAddressProvider() {
		return Stream.of(
			Arguments.of("Only address1", "Storgatan 1", null, "Storgatan 1"),
			Arguments.of("address1 preferred over address2", "Storgatan 1", "Box 123", "Storgatan 1"),
			Arguments.of("Null address1 falls back to address2", null, "Box 123", "Box 123"),
			Arguments.of("Blank address1 falls back to address2", " ", "Box 123", "Box 123"));
	}

	public static Stream<Arguments> incompletePostAddressProvider() {
		return Stream.of(
			Arguments.of("Null address1 and address2", new LEPostAddress().postalCode("85222").city("Sundsvall")),
			Arguments.of("Blank address1 and address2", new LEPostAddress().address1(" ").address2(" ").postalCode("85222").city("Sundsvall")),
			Arguments.of("Null postal code", new LEPostAddress().address1("Storgatan 1").city("Sundsvall")),
			Arguments.of("Blank postal code", new LEPostAddress().address1("Storgatan 1").postalCode(" ").city("Sundsvall")),
			Arguments.of("Null city", new LEPostAddress().address1("Storgatan 1").postalCode("85222")),
			Arguments.of("Blank city", new LEPostAddress().address1("Storgatan 1").postalCode("85222").city(" ")));
	}
}
