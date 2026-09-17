package se.sundsvall.postportalservice.integration.messaging.configuration;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.postportalservice.Application;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
class MessagingPropertiesTest {

	@Autowired
	private MessagingProperties properties;

	@Test
	void testProperties() {
		assertThat(properties.connectTimeout()).isEqualTo(10);
		assertThat(properties.readTimeout()).isEqualTo(20);
		assertThat(properties.callbackEmailSender().name()).isEqualTo("Postportalen");
		assertThat(properties.callbackEmailSender().addresses()).containsExactlyInAnyOrderEntriesOf(Map.of(
			"2281", "noreply@postportal.se",
			"2260", "ange@ange.se"));
	}
}
