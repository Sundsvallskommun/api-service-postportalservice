package se.sundsvall.postportalservice.service.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static se.sundsvall.postportalservice.api.model.Recipient.DeliveryMethod.DIGITAL_MAIL;

import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

class ParseUtilTest {

	@Test
	void parseLetterRequest_OK() {
		var requestString = """
			{
			    "addresses": [
			        {
			            "apartmentNumber": "1101",
			            "careOf": "c/o John Doe",
			            "city": "Sundsvall",
			            "country": "Sweden",
			            "firstName": "John",
			            "lastName": "Doe",
			            "street": "Main Street 1",
			            "zipCode": "12345"
			        }
			    ],
			    "body": "This is the body of the letter",
			    "contentType": "text/plain",
			    "recipients": [
			        {
			            "address": {
			                "apartmentNumber": "1101",
			                "careOf": "c/o John Doe",
			                "city": "Sundsvall",
			                "country": "Sweden",
			                "firstName": "John",
			                "lastName": "Doe",
			                "street": "Main Street 1",
			                "zipCode": "12345"
			            },
			            "deliveryMethod": "DIGITAL_MAIL",
			            "partyId": "123e4567-e89b-12d3-a456-426614174000"
			        }
			    ],
			    "subject": "This is the subject of the letter"
			}
			""";

		var letterRequest = ParseUtil.parseLetterRequest(requestString);

		assertThat(letterRequest).isNotNull();
		assertThat(letterRequest.getSubject()).isEqualTo("This is the subject of the letter");
		assertThat(letterRequest.getBody()).isEqualTo("This is the body of the letter");
		assertThat(letterRequest.getContentType()).isEqualTo("text/plain");
		assertThat(letterRequest.getRecipients().getFirst().getDeliveryMethod()).isEqualTo(DIGITAL_MAIL);
		assertThat(letterRequest.getRecipients().getFirst().getPartyId()).isEqualTo("123e4567-e89b-12d3-a456-426614174000");
		assertThat(letterRequest.getRecipients().getFirst().getAddress().getFirstName()).isEqualTo("John");
		assertThat(letterRequest.getRecipients().getFirst().getAddress().getLastName()).isEqualTo("Doe");
		assertThat(letterRequest.getRecipients().getFirst().getAddress().getStreet()).isEqualTo("Main Street 1");
		assertThat(letterRequest.getRecipients().getFirst().getAddress().getApartmentNumber()).isEqualTo("1101");
		assertThat(letterRequest.getRecipients().getFirst().getAddress().getCareOf()).isEqualTo("c/o John Doe");
		assertThat(letterRequest.getRecipients().getFirst().getAddress().getZipCode()).isEqualTo("12345");
		assertThat(letterRequest.getRecipients().getFirst().getAddress().getCity()).isEqualTo("Sundsvall");
		assertThat(letterRequest.getRecipients().getFirst().getAddress().getCountry()).isEqualTo("Sweden");

		assertThat(letterRequest.getAddresses().getFirst().getFirstName()).isEqualTo("John");
		assertThat(letterRequest.getAddresses().getFirst().getLastName()).isEqualTo("Doe");
		assertThat(letterRequest.getAddresses().getFirst().getStreet()).isEqualTo("Main Street 1");
		assertThat(letterRequest.getAddresses().getFirst().getApartmentNumber()).isEqualTo("1101");
		assertThat(letterRequest.getAddresses().getFirst().getCareOf()).isEqualTo("c/o John Doe");
		assertThat(letterRequest.getAddresses().getFirst().getZipCode()).isEqualTo("12345");
		assertThat(letterRequest.getAddresses().getFirst().getCity()).isEqualTo("Sundsvall");
		assertThat(letterRequest.getAddresses().getFirst().getCountry()).isEqualTo("Sweden");
	}

	@Test
	void parseLetterRequest_InvalidJson() {
		var requestString = "Invalid JSON string";

		assertThatThrownBy(() -> ParseUtil.parseLetterRequest(requestString))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Couldn't parse letter request");
	}

	@Test
	void parseDigitalRegisteredLetterRequest() {
		var requestString = """
			{
				"partyId": "6d0773d6-3e7f-4552-81bc-f0007af95adf",
				"subject": "This is the subject of the letter"
			}
			""";

		var digitalRegisteredLetterRequest = ParseUtil.parseDigitalRegisteredLetterRequest(requestString);

		assertThat(digitalRegisteredLetterRequest).isNotNull();
		assertThat(digitalRegisteredLetterRequest.getPartyId()).isEqualTo("6d0773d6-3e7f-4552-81bc-f0007af95adf");
		assertThat(digitalRegisteredLetterRequest.getSubject()).isEqualTo("This is the subject of the letter");
	}

	@Test
	void parseDigitalRegisteredLetterRequest_InvalidJson() {
		var requestString = "Invalid JSON string";

		assertThatThrownBy(() -> ParseUtil.parseDigitalRegisteredLetterRequest(requestString))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Couldn't parse digital registered letter request");
	}

}
