package se.sundsvall.postportalservice.api.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class MessageDetailsTest {

	// MessageDetails attributes
	private static final String SUBJECT = "subject";
	private static final String BODY = "body";
	private static final LocalDateTime SENT_AT = now();
	private static final SigningStatus SIGNING_STATUS = new SigningStatus();
	private static final List<MessageDetails.AttachmentDetails> ATTACHMENTS = List.of(new MessageDetails.AttachmentDetails());
	private static final List<MessageDetails.RecipientDetails> RECIPIENTS = List.of(new MessageDetails.RecipientDetails());

	// MessageDetails.Attachment attributes
	private static final String ATTACHMENT_ID = "attachmentId";
	private static final String FILE_NAME = "fileName";
	private static final String CONTENT_TYPE = "contentType";

	// MessageDetails.Recipient attributes
	private static final String NAME = "name";
	private static final String PARTY_ID = "partyId";
	private static final String MOBILE_NUMBER = "mobileNumber";
	private static final String STREET_ADDRESS = "streetAddress";
	private static final String ZIP_CODE = "zipCode";
	private static final String CITY = "city";
	private static final String MESSAGE_TYPE = "messageType";
	private static final String STATUS = "status";

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), LocalDateTime.class);
	}

	@Test
	void testMessageDetails() {
		org.hamcrest.MatcherAssert.assertThat(MessageDetails.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testMessageDetailsAttachment() {
		org.hamcrest.MatcherAssert.assertThat(MessageDetails.AttachmentDetails.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testMessageDetailsRecipient() {
		org.hamcrest.MatcherAssert.assertThat(MessageDetails.RecipientDetails.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void MessageDetails_getterAndSetterTest() {
		final var bean = new MessageDetails();
		bean.setSubject(SUBJECT);
		bean.setBody(BODY);
		bean.setSentAt(SENT_AT);
		bean.setSigningStatus(SIGNING_STATUS);
		bean.setAttachments(ATTACHMENTS);
		bean.setRecipients(RECIPIENTS);

		assertThat(bean.getSubject()).isEqualTo(SUBJECT);
		assertThat(bean.getBody()).isEqualTo(BODY);
		assertThat(bean.getSentAt()).isEqualTo(SENT_AT);
		assertThat(bean.getSigningStatus()).isEqualTo(SIGNING_STATUS);
		assertThat(bean.getAttachments()).isEqualTo(ATTACHMENTS);
		assertThat(bean.getRecipients()).isEqualTo(RECIPIENTS);
	}

	@Test
	void MessageDetails_AttachmentDetails_getterAndSetterTest() {
		final var bean = new MessageDetails.AttachmentDetails();
		bean.setAttachmentId(ATTACHMENT_ID);
		bean.setFileName(FILE_NAME);
		bean.setContentType(CONTENT_TYPE);

		assertThat(bean.getAttachmentId()).isEqualTo(ATTACHMENT_ID);
		assertThat(bean.getFileName()).isEqualTo(FILE_NAME);
		assertThat(bean.getContentType()).isEqualTo(CONTENT_TYPE);
	}

	@Test
	void MessageDetails_RecipientDetails_getterAndSetterTest() {
		final var bean = new MessageDetails.RecipientDetails();
		bean.setName(NAME);
		bean.setPartyId(PARTY_ID);
		bean.setMobileNumber(MOBILE_NUMBER);
		bean.setStreetAddress(STREET_ADDRESS);
		bean.setZipCode(ZIP_CODE);
		bean.setCity(CITY);
		bean.setMessageType(MESSAGE_TYPE);
		bean.setStatus(STATUS);

		assertThat(bean.getName()).isEqualTo(NAME);
		assertThat(bean.getPartyId()).isEqualTo(PARTY_ID);
		assertThat(bean.getMobileNumber()).isEqualTo(MOBILE_NUMBER);
		assertThat(bean.getStreetAddress()).isEqualTo(STREET_ADDRESS);
		assertThat(bean.getZipCode()).isEqualTo(ZIP_CODE);
		assertThat(bean.getCity()).isEqualTo(CITY);
		assertThat(bean.getMessageType()).isEqualTo(MESSAGE_TYPE);
		assertThat(bean.getStatus()).isEqualTo(STATUS);
	}

	@Test
	void MessageDetails_builderPatternTest() {
		final var bean = MessageDetails.create()
			.withSubject(SUBJECT)
			.withBody(BODY)
			.withSentAt(SENT_AT)
			.withSigningStatus(SIGNING_STATUS)
			.withAttachments(ATTACHMENTS)
			.withRecipients(RECIPIENTS);

		assertThat(bean.getSubject()).isEqualTo(SUBJECT);
		assertThat(bean.getBody()).isEqualTo(BODY);
		assertThat(bean.getSentAt()).isEqualTo(SENT_AT);
		assertThat(bean.getSigningStatus()).isEqualTo(SIGNING_STATUS);
		assertThat(bean.getAttachments()).isEqualTo(ATTACHMENTS);
		assertThat(bean.getRecipients()).isEqualTo(RECIPIENTS);
	}

	@Test
	void MessageDetails_AttachmentDetails_builderPatternTest() {
		final var bean = MessageDetails.AttachmentDetails.create()
			.withAttachmentId(ATTACHMENT_ID)
			.withFileName(FILE_NAME)
			.withContentType(CONTENT_TYPE);

		assertThat(bean.getAttachmentId()).isEqualTo(ATTACHMENT_ID);
		assertThat(bean.getFileName()).isEqualTo(FILE_NAME);
		assertThat(bean.getContentType()).isEqualTo(CONTENT_TYPE);
	}

	@Test
	void MessageDetails_RecipientDetails_builderPatternTest() {
		final var bean = MessageDetails.RecipientDetails.create()
			.withName(NAME)
			.withPartyId(PARTY_ID)
			.withMobileNumber(MOBILE_NUMBER)
			.withStreetAddress(STREET_ADDRESS)
			.withZipCode(ZIP_CODE)
			.withCity(CITY)
			.withMessageType(MESSAGE_TYPE)
			.withStatus(STATUS);

		assertThat(bean.getName()).isEqualTo(NAME);
		assertThat(bean.getPartyId()).isEqualTo(PARTY_ID);
		assertThat(bean.getMobileNumber()).isEqualTo(MOBILE_NUMBER);
		assertThat(bean.getStreetAddress()).isEqualTo(STREET_ADDRESS);
		assertThat(bean.getZipCode()).isEqualTo(ZIP_CODE);
		assertThat(bean.getCity()).isEqualTo(CITY);
		assertThat(bean.getMessageType()).isEqualTo(MESSAGE_TYPE);
		assertThat(bean.getStatus()).isEqualTo(STATUS);
	}

	@Test
	void MessageDetails_constructorTest() {
		assertThat(new MessageDetails()).hasAllNullFieldsOrProperties();
		assertThat(new MessageDetails()).hasOnlyFields("subject", "body", "sentAt", "signingStatus", "attachments", "recipients");
	}

	@Test
	void MessageDetails_AttachmentDetails_constructorTest() {
		assertThat(new MessageDetails.AttachmentDetails()).hasAllNullFieldsOrProperties();
		assertThat(new MessageDetails.AttachmentDetails()).hasOnlyFields("attachmentId", "fileName", "contentType");
	}

	@Test
	void MessageDetails_RecipientDetails_constructorTest() {
		assertThat(new MessageDetails.RecipientDetails()).hasAllNullFieldsOrProperties();
		assertThat(new MessageDetails.RecipientDetails()).hasOnlyFields("name", "partyId", "legalId", "mobileNumber", "streetAddress", "zipCode", "city", "messageType", "status");
	}

}
