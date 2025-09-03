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

import java.sql.Blob;
import java.time.OffsetDateTime;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AttachmentEntityTest {

	private static final String ID = "123e4567-e89b-12d3-a456-426614174000";
	private static final String FILE_NAME = "attachment.txt";
	private static final String CONTENT_TYPE = "text/plain";
	private static final OffsetDateTime CREATED = now();
	private final Blob blobMock = Mockito.mock(Blob.class);

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(AttachmentEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void setterAndGetterTest() {
		var attachmentEntity = new AttachmentEntity();

		attachmentEntity.setId(ID);
		attachmentEntity.setFileName(FILE_NAME);
		attachmentEntity.setContentType(CONTENT_TYPE);
		attachmentEntity.setContent(blobMock);
		attachmentEntity.setCreated(CREATED);

		assertThat(attachmentEntity.getId()).isEqualTo(ID);
		assertThat(attachmentEntity.getFileName()).isEqualTo(FILE_NAME);
		assertThat(attachmentEntity.getContentType()).isEqualTo(CONTENT_TYPE);
		assertThat(attachmentEntity.getContent()).isEqualTo(blobMock);
		assertThat(attachmentEntity.getCreated()).isEqualTo(CREATED);
		assertThat(attachmentEntity).hasNoNullFieldsOrProperties();
	}

	@Test
	void constructorTest() {
		var attachmentEntity = new AttachmentEntity();
		assertThat(attachmentEntity).hasAllNullFieldsOrPropertiesExcept();
	}
}
