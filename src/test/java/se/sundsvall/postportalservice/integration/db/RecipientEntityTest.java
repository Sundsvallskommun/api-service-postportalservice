package se.sundsvall.postportalservice.integration.db;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.OffsetDateTime;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.sundsvall.postportalservice.integration.db.converter.MessageType;

class RecipientEntityTest {

	private static final String ID = "id";
	private static final String EXTERNAL_ID = "externalId";
	private static final String PARTY_ID = "partyId";
	private static final String EMAIL = "email";
	private static final String PHONE_NUMBER = "phoneNumber";
	private static final String FIRST_NAME = "firstName";
	private static final String LAST_NAME = "lastName";
	private static final String STREET_ADDRESS = "streetAddress";
	private static final String APARTMENT_NUMBER = "apartmentNumber";
	private static final String CARE_OF = "careOf";
	private static final String ZIP_CODE = "zipCode";
	private static final String CITY = "city";
	private static final String COUNTRY = "country";
	private static final String STATUS_DETAIL = "statusDetail";
	private static final String MESSAGE_STATUS = "SENT";
	private static final MessageType MESSAGE_TYPE = MessageType.SNAIL_MAIL;
	private static final OffsetDateTime CREATED = now();

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(RecipientEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderPattern() {
		var recipientEntity = RecipientEntity.create()
			.withId(ID)
			.withPartyId(PARTY_ID)
			.withEmail(EMAIL)
			.withPhoneNumber(PHONE_NUMBER)
			.withFirstName(FIRST_NAME)
			.withLastName(LAST_NAME)
			.withStreetAddress(STREET_ADDRESS)
			.withApartmentNumber(APARTMENT_NUMBER)
			.withCareOf(CARE_OF)
			.withZipCode(ZIP_CODE)
			.withCity(CITY)
			.withCountry(COUNTRY)
			.withCreated(CREATED)
			.withStatusDetail(STATUS_DETAIL)
			.withMessageType(MESSAGE_TYPE)
			.withStatus(MESSAGE_STATUS)
			.withExternalId(EXTERNAL_ID);

		assertThat(recipientEntity.getId()).isEqualTo(ID);
		assertThat(recipientEntity.getPartyId()).isEqualTo(PARTY_ID);
		assertThat(recipientEntity.getEmail()).isEqualTo(EMAIL);
		assertThat(recipientEntity.getPhoneNumber()).isEqualTo(PHONE_NUMBER);
		assertThat(recipientEntity.getFirstName()).isEqualTo(FIRST_NAME);
		assertThat(recipientEntity.getLastName()).isEqualTo(LAST_NAME);
		assertThat(recipientEntity.getStreetAddress()).isEqualTo(STREET_ADDRESS);
		assertThat(recipientEntity.getApartmentNumber()).isEqualTo(APARTMENT_NUMBER);
		assertThat(recipientEntity.getCareOf()).isEqualTo(CARE_OF);
		assertThat(recipientEntity.getZipCode()).isEqualTo(ZIP_CODE);
		assertThat(recipientEntity.getCity()).isEqualTo(CITY);
		assertThat(recipientEntity.getCountry()).isEqualTo(COUNTRY);
		assertThat(recipientEntity.getCreated()).isEqualTo(CREATED);
		assertThat(recipientEntity.getStatusDetail()).isEqualTo(STATUS_DETAIL);
		assertThat(recipientEntity.getMessageType()).isEqualTo(MESSAGE_TYPE);
		assertThat(recipientEntity.getStatus()).isEqualTo(MESSAGE_STATUS);
		assertThat(recipientEntity.getExternalId()).isEqualTo(EXTERNAL_ID);
		assertThat(recipientEntity).hasNoNullFieldsOrProperties();
	}

	@Test
	void getterAndSetterTest() {
		var recipientEntity = new RecipientEntity();

		recipientEntity.setId(ID);
		recipientEntity.setPartyId(PARTY_ID);
		recipientEntity.setEmail(EMAIL);
		recipientEntity.setPhoneNumber(PHONE_NUMBER);
		recipientEntity.setFirstName(FIRST_NAME);
		recipientEntity.setLastName(LAST_NAME);
		recipientEntity.setStreetAddress(STREET_ADDRESS);
		recipientEntity.setApartmentNumber(APARTMENT_NUMBER);
		recipientEntity.setCareOf(CARE_OF);
		recipientEntity.setZipCode(ZIP_CODE);
		recipientEntity.setCity(CITY);
		recipientEntity.setCountry(COUNTRY);
		recipientEntity.setCreated(CREATED);
		recipientEntity.setStatusDetail(STATUS_DETAIL);
		recipientEntity.setMessageType(MESSAGE_TYPE);
		recipientEntity.setStatus(MESSAGE_STATUS);
		recipientEntity.setExternalId(EXTERNAL_ID);

		assertThat(recipientEntity.getId()).isEqualTo(ID);
		assertThat(recipientEntity.getPartyId()).isEqualTo(PARTY_ID);
		assertThat(recipientEntity.getEmail()).isEqualTo(EMAIL);
		assertThat(recipientEntity.getPhoneNumber()).isEqualTo(PHONE_NUMBER);
		assertThat(recipientEntity.getFirstName()).isEqualTo(FIRST_NAME);
		assertThat(recipientEntity.getLastName()).isEqualTo(LAST_NAME);
		assertThat(recipientEntity.getStreetAddress()).isEqualTo(STREET_ADDRESS);
		assertThat(recipientEntity.getApartmentNumber()).isEqualTo(APARTMENT_NUMBER);
		assertThat(recipientEntity.getCareOf()).isEqualTo(CARE_OF);
		assertThat(recipientEntity.getZipCode()).isEqualTo(ZIP_CODE);
		assertThat(recipientEntity.getCity()).isEqualTo(CITY);
		assertThat(recipientEntity.getCountry()).isEqualTo(COUNTRY);
		assertThat(recipientEntity.getCreated()).isEqualTo(CREATED);
		assertThat(recipientEntity.getStatusDetail()).isEqualTo(STATUS_DETAIL);
		assertThat(recipientEntity.getMessageType()).isEqualTo(MESSAGE_TYPE);
		assertThat(recipientEntity.getStatus()).isEqualTo(MESSAGE_STATUS);
		assertThat(recipientEntity.getExternalId()).isEqualTo(EXTERNAL_ID);
		assertThat(recipientEntity).hasNoNullFieldsOrProperties();
	}

	@Test
	void constructorTest() {
		var recipientEntity = new RecipientEntity();
		assertThat(recipientEntity).hasAllNullFieldsOrPropertiesExcept("deliveries");
	}

}
