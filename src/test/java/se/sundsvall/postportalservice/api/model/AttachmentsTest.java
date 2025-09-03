package se.sundsvall.postportalservice.api.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.multipart.MultipartFile;

class AttachmentsTest {

	@Test
	void testBean() {
		org.hamcrest.MatcherAssert.assertThat(Attachments.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void attachmentsBuilderTest() {
		var multiPartFile = Mockito.mock(MultipartFile.class);

		var files = List.of(multiPartFile);

		var attachments = Attachments.create()
			.withFiles(files);

		assertThat(attachments).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(attachments.getFiles()).isEqualTo(files);
	}

	@Test
	void attachmentsConstructorTest() {
		var multiPartFile = Mockito.mock(MultipartFile.class);

		var files = List.of(multiPartFile);

		var attachments = new Attachments();
		attachments.setFiles(files);

		assertThat(attachments).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(attachments.getFiles()).isEqualTo(files);
	}
}
