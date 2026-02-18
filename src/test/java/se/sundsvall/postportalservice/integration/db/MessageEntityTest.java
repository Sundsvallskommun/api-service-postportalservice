package se.sundsvall.postportalservice.integration.db;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.sundsvall.postportalservice.integration.db.converter.MessageType;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class MessageEntityTest {

	private static final String ID = "id";
	private static final String MUNICIPALITY_ID = "municipalityId";
	private static final String DISPLAY_NAME = "displayName";
	private static final MessageType MESSAGE_TYPE = MessageType.SMS;
	private static final String BODY = "body";
	private static final String CONTENT_TYPE = "contentType";
	private static final String SUBJECT = "subject";
	private static final OffsetDateTime CREATED = now();

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(MessageEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("attachments"),
			hasValidBeanEqualsExcluding("attachments"),
			hasValidBeanToStringExcluding("attachments")));
	}

	@Test
	void builderPattern() {
		var userEntity = new UserEntity();
		var departmentEntity = new DepartmentEntity();
		var attachments = new ArrayList<AttachmentEntity>();
		var recipientEntities = new ArrayList<RecipientEntity>();

		var messageEntity = MessageEntity.create()
			.withId(ID)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withMessageType(MESSAGE_TYPE)
			.withBody(BODY)
			.withContentType(CONTENT_TYPE)
			.withSubject(SUBJECT)
			.withCreated(CREATED)
			.withDisplayName(DISPLAY_NAME)
			.withUser(userEntity)
			.withDepartment(departmentEntity)
			.withAttachments(attachments)
			.withRecipients(recipientEntities);

		assertThat(messageEntity.getId()).isEqualTo(ID);
		assertThat(messageEntity.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(messageEntity.getMessageType()).isEqualTo(MESSAGE_TYPE);
		assertThat(messageEntity.getBody()).isEqualTo(BODY);
		assertThat(messageEntity.getContentType()).isEqualTo(CONTENT_TYPE);
		assertThat(messageEntity.getSubject()).isEqualTo(SUBJECT);
		assertThat(messageEntity.getCreated()).isEqualTo(CREATED);
		assertThat(messageEntity.getDisplayName()).isEqualTo(DISPLAY_NAME);
		assertThat(messageEntity.getUser()).isEqualTo(userEntity);
		assertThat(messageEntity.getDepartment()).isEqualTo(departmentEntity);
		assertThat(messageEntity.getAttachments()).isEqualTo(attachments);
		assertThat(messageEntity.getRecipients()).isEqualTo(recipientEntities);
		assertThat(messageEntity).hasNoNullFieldsOrProperties();
	}

	@Test
	void getterAndSetterTest() {
		var messageEntity = new MessageEntity();
		var userEntity = new UserEntity();
		var departmentEntity = new DepartmentEntity();
		var attachments = new ArrayList<AttachmentEntity>();
		var recipientEntities = new ArrayList<RecipientEntity>();

		messageEntity.setId(ID);
		messageEntity.setMunicipalityId(MUNICIPALITY_ID);
		messageEntity.setMessageType(MESSAGE_TYPE);
		messageEntity.setBody(BODY);
		messageEntity.setContentType(CONTENT_TYPE);
		messageEntity.setSubject(SUBJECT);
		messageEntity.setCreated(CREATED);
		messageEntity.setDisplayName(DISPLAY_NAME);
		messageEntity.setUser(userEntity);
		messageEntity.setDepartment(departmentEntity);
		messageEntity.setAttachments(attachments);
		messageEntity.setRecipients(recipientEntities);

		assertThat(messageEntity.getId()).isEqualTo(ID);
		assertThat(messageEntity.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(messageEntity.getMessageType()).isEqualTo(MESSAGE_TYPE);
		assertThat(messageEntity.getBody()).isEqualTo(BODY);
		assertThat(messageEntity.getContentType()).isEqualTo(CONTENT_TYPE);
		assertThat(messageEntity.getSubject()).isEqualTo(SUBJECT);
		assertThat(messageEntity.getCreated()).isEqualTo(CREATED);
		assertThat(messageEntity.getDisplayName()).isEqualTo(DISPLAY_NAME);
		assertThat(messageEntity.getUser()).isEqualTo(userEntity);
		assertThat(messageEntity.getDepartment()).isEqualTo(departmentEntity);
		assertThat(messageEntity.getAttachments()).isEqualTo(attachments);
		assertThat(messageEntity.getRecipients()).isEqualTo(recipientEntities);
		assertThat(messageEntity).hasNoNullFieldsOrProperties();
	}

	@Test
	void constructorTest() {
		var messageEntity = new MessageEntity();
		assertThat(messageEntity).hasAllNullFieldsOrPropertiesExcept("attachments", "recipients");
	}
}
