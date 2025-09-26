package se.sundsvall.postportalservice;

import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import java.util.ArrayList;
import java.util.List;
import org.mariadb.jdbc.MariaDbBlob;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.postportalservice.api.model.Address;
import se.sundsvall.postportalservice.api.model.Attachments;
import se.sundsvall.postportalservice.api.model.DigitalRegisteredLetterRequest;
import se.sundsvall.postportalservice.api.model.LetterRequest;
import se.sundsvall.postportalservice.api.model.PrecheckRequest;
import se.sundsvall.postportalservice.api.model.Recipient;
import se.sundsvall.postportalservice.api.model.SmsRecipient;
import se.sundsvall.postportalservice.api.model.SmsRequest;
import se.sundsvall.postportalservice.integration.digitalregisteredletter.AttachmentMultipartFile;

public final class TestDataFactory {

	public static final String MUNICIPALITY_ID = "2281";
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
			.withPhoneNumber("+46123456789")
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

	public static PrecheckRequest createValidPrecheckRequest() {
		return PrecheckRequest.create()
			.withRecipients(List.of("6d0773d6-3e7f-4552-81bc-f0007af95adf"));
	}

	public static Attachments createValidAttachments(int numberOfAttachments) throws Exception {
		var fileName = "test%s.pdf";
		var contentType = "application/pdf";
		var content = "Dummy PDF content".getBytes();
		var blob = new MariaDbBlob(content);

		List<MultipartFile> files = new ArrayList<>();
		for (int i = 1; i <= numberOfAttachments; i++) {
			final var attachment = new AttachmentMultipartFile(fileName.formatted(i), contentType, blob);
			files.add(attachment);
		}

		return Attachments.create()
			.withFiles(files);
	}
}
