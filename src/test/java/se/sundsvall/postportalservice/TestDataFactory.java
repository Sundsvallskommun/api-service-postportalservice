package se.sundsvall.postportalservice;

import generated.se.sundsvall.messaging.Mailbox;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import se.sundsvall.postportalservice.api.model.Address;
import se.sundsvall.postportalservice.api.model.DigitalRegisteredLetterRequest;
import se.sundsvall.postportalservice.api.model.LetterCsvRequest;
import se.sundsvall.postportalservice.api.model.LetterRequest;
import se.sundsvall.postportalservice.api.model.Recipient;
import se.sundsvall.postportalservice.api.model.SmsRecipient;
import se.sundsvall.postportalservice.api.model.SmsRequest;

import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

public final class TestDataFactory {

	public static final String MUNICIPALITY_ID = "2281";
	public static final String MOBILE_NUMBER = "+46701740605";
	public static final String SUNDSVALL_MUNICIPALITY_ORG_NO = "2120002411";
	public static final String INVALID_MUNICIPALITY_ID = "NOT_A_VALID_MUNICIPALITY_ID";

	private TestDataFactory() {}

	public static Recipient createValidRecipient() {
		return Recipient.create()
			.withAddress(createValidAddress())
			.withPartyId("6d0773d6-3e7f-4552-81bc-f0007af95adf")
			.withDeliveryMethod(Recipient.DeliveryMethod.DIGITAL_MAIL);
	}

	public static Address createValidAddress() {
		return Address.create()
			.withFirstName("John")
			.withLastName("Doe")
			.withStreet("Main Street 1")
			.withApartmentNumber("1101")
			.withCareOf("c/o Jane Doe")
			.withZipCode("12345")
			.withCity("Sundsvall")
			.withCountry("Sweden");
	}

	public static SmsRecipient createValidSmsRecipient() {
		return SmsRecipient.create()
			.withPhoneNumber(MOBILE_NUMBER)
			.withPartyId("6d0773d6-3e7f-4552-81bc-f0007af95adf");
	}

	public static DigitalRegisteredLetterRequest createValidDigitalRegisteredLetterRequest() {
		return DigitalRegisteredLetterRequest.create()
			.withContentType(TEXT_PLAIN_VALUE)
			.withBody("This is the body of the letter")
			.withSubject("Test Subject")
			.withPartyId("6d0773d6-3e7f-4552-81bc-f0007af95adf");
	}

	public static SmsRequest createValidSmsRequest() {
		return SmsRequest.create()
			.withMessage("This is a test message")
			.withRecipients(List.of(createValidSmsRecipient()));
	}

	public static LetterRequest createValidLetterRequest() {
		return LetterRequest.create()
			.withBody("body")
			.withSubject("Test Subject")
			.withRecipients(List.of(createValidRecipient()))
			.withContentType("text/plain")
			.withAddresses(List.of(createValidAddress()));
	}

	public static LetterCsvRequest createValidLetterCsvRequest() {
		return LetterCsvRequest.create()
			.withBody("body")
			.withSubject("Test Subject")
			.withContentType("text/plain");
	}

	public static Mailbox createMailbox(String partyId, Boolean isReachable, String reason) {
		final var mailbox = new Mailbox();

		mailbox.setPartyId(partyId);
		mailbox.setReachable(isReachable);
		mailbox.setReason(reason);

		return mailbox;
	}

	public static Mailbox createMailbox(String partyId, Boolean isReachable) {
		return createMailbox(partyId, isReachable, null);
	}

	/**
	 * Generates a valid Swedish legal ID (personnummer) for a person of specified age.
	 * Format: YYYYMMDDXXXX (12 digits)
	 *
	 * @param  yearsOld the age of the person in years
	 * @param  suffix   the 4-digit suffix to make the ID unique
	 * @return          a 12-digit legal ID string
	 */
	public static String generateLegalId(final int yearsOld, final String suffix) {
		final var birthDate = LocalDate.now().minusYears(yearsOld);
		final var dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
		return birthDate.format(dateFormatter) + suffix;
	}
}
