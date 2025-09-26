package se.sundsvall.postportalservice.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

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
			.withPhoneNumber("+46123456789");

		var result = entityMapper.toRecipientEntity(smsRecipient);

		assertThat(result).isNotNull();
		assertThat(result.getPartyId()).isEqualTo(smsRecipient.getPartyId());
		assertThat(result.getPhoneNumber()).isEqualTo(smsRecipient.getPhoneNumber());
		assertThat(result.getMessageType()).isEqualTo(MessageType.SMS);
		assertThat(result.getStatus()).isEqualTo("PENDING");
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
		assertThat(result.getStatus()).isEqualTo("PENDING");
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
		assertThat(result.getStatus()).isEqualTo("PENDING");
		assertThat(result.getFirstName()).isEqualTo(address.getFirstName());
		assertThat(result.getLastName()).isEqualTo(address.getLastName());
		assertThat(result.getStreetAddress()).isEqualTo(address.getStreet());
		assertThat(result.getApartmentNumber()).isEqualTo(address.getApartmentNumber());
		assertThat(result.getCareOf()).isEqualTo(address.getCareOf());
		assertThat(result.getZipCode()).isEqualTo(address.getZipCode());
		assertThat(result.getCity()).isEqualTo(address.getCity());
		assertThat(result.getCountry()).isEqualTo(address.getCountry());
	}
}
