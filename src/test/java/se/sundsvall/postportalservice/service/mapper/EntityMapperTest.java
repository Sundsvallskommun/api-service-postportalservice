package se.sundsvall.postportalservice.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.postportalservice.Constants.PENDING;
import static se.sundsvall.postportalservice.Constants.UNDELIVERABLE;
import static se.sundsvall.postportalservice.TestDataFactory.MOBILE_NUMBER;
import static se.sundsvall.postportalservice.integration.db.converter.MessageType.LETTER;

import generated.se.sundsvall.citizen.CitizenAddress;
import generated.se.sundsvall.citizen.CitizenExtended;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.postportalservice.api.model.Address;
import se.sundsvall.postportalservice.api.model.Recipient;
import se.sundsvall.postportalservice.api.model.SmsRecipient;
import se.sundsvall.postportalservice.integration.db.converter.MessageType;

@ExtendWith(MockitoExtension.class)
class EntityMapperTest {

	@InjectMocks
	private EntityMapper entityMapper;

	@Test
	void toRecipientEntity_smsRecipient() {
		var smsRecipient = new SmsRecipient()
			.withPartyId("00000000-0000-0000-0000-000000000001")
			.withPhoneNumber(MOBILE_NUMBER);

		var result = entityMapper.toRecipientEntity(smsRecipient);

		assertThat(result).isNotNull();
		assertThat(result.getPartyId()).isEqualTo(smsRecipient.getPartyId());
		assertThat(result.getPhoneNumber()).isEqualTo(smsRecipient.getPhoneNumber());
		assertThat(result.getMessageType()).isEqualTo(MessageType.SMS);
		assertThat(result.getStatus()).isEqualTo(PENDING);
	}

	@Test
	void toRecipientEntity_recipient_digitalMail() {
		var recipient = new Recipient()
			.withPartyId("00000000-0000-0000-0000-000000000001")
			.withDeliveryMethod(Recipient.DeliveryMethod.DIGITAL_MAIL);

		var result = entityMapper.toRecipientEntity(recipient);

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
		var address = new Address()
			.withFirstName("First")
			.withLastName("Last")
			.withStreet("Street 1")
			.withApartmentNumber("2A")
			.withCareOf("c/o Someone")
			.withZipCode("12345")
			.withCity("City")
			.withCountry("Country");
		var recipient = new Recipient()
			.withPartyId("00000000-0000-0000-0000-000000000001")
			.withAddress(address)
			.withDeliveryMethod(Recipient.DeliveryMethod.SNAIL_MAIL);

		var result = entityMapper.toRecipientEntity(recipient);

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
	void toRecipientEntity_fromAddress() {
		var address = new Address()
			.withFirstName("john")
			.withLastName("doe")
			.withStreet("Main street 1")
			.withApartmentNumber("3B")
			.withCareOf("c/o Someone")
			.withZipCode("54321")
			.withCity("Town")
			.withCountry("Countryland");

		var result = entityMapper.toRecipientEntity(address);

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
	void toDigitalMailRecipientEntity() {
		var partyId = "00000000-0000-0000-0000-000000000001";

		var result = entityMapper.toDigitalMailRecipientEntity(partyId);

		assertThat(result).isNotNull();
		assertThat(result.getPartyId()).isEqualTo(partyId);
		assertThat(result.getMessageType()).isEqualTo(MessageType.DIGITAL_MAIL);
		assertThat(result.getStatus()).isEqualTo(PENDING);
	}

	@Test
	void toSnailMailRecipientEntity() {
		var citizenExtended = new CitizenExtended()
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

		var result = entityMapper.toSnailMailRecipientEntity(citizenExtended);

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
		var citizenExtended = new CitizenExtended()
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

		var result = entityMapper.toSnailMailRecipientEntity(citizenExtended);

		assertThat(result).isNull();
	}

	@Test
	void toUndeliverableRecipientEntity() {
		var partyId = UUID.randomUUID();
		var citizenExtended = new CitizenExtended()
			.personId(partyId);

		var result = entityMapper.toUndeliverableRecipientEntity(citizenExtended);

		assertThat(result).isNotNull();
		assertThat(result.getPartyId()).isEqualTo(partyId.toString());
		assertThat(result.getMessageType()).isEqualTo(LETTER);
		assertThat(result.getStatus()).isEqualTo(UNDELIVERABLE);
	}
}
