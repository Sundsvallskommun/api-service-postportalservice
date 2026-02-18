package se.sundsvall.postportalservice.api.model;

import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.dept44.models.api.paging.PagingMetaData;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class MessagesTest {

	private static final List<Message> MESSAGES = List.of(new Message());
	private static final PagingMetaData META_DATA = PagingMetaData.create();

	@Test
	void testBean() {
		org.hamcrest.MatcherAssert.assertThat(Messages.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void getterAndSetterTest() {
		final var bean = new Messages();
		bean.setMetaData(META_DATA);
		bean.setMessages(MESSAGES);

		assertThat(bean.getMessages()).isEqualTo(MESSAGES);
		assertThat(bean.getMetaData()).isEqualTo(META_DATA);
	}

	@Test
	void builderPatternTest() {
		final var bean = Messages.create()
			.withMetaData(META_DATA)
			.withMessages(MESSAGES);

		assertThat(bean.getMessages()).isEqualTo(MESSAGES);
		assertThat(bean.getMetaData()).isEqualTo(META_DATA);
	}

	@Test
	void constructorTest() {
		assertThat(new Messages()).hasAllNullFieldsOrProperties();
		assertThat(new Messages()).hasOnlyFields("metaData", "messages");
	}

}
