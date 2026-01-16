package se.sundsvall.postportalservice.integration.party.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.postportalservice.Application;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
class PartyPropertiesTest {

	@Autowired
	private PartyProperties properties;

	@Test
	void testProperties() {
		assertThat(properties.connectTimeout()).isEqualTo(10);
		assertThat(properties.readTimeout()).isEqualTo(20);
	}
}
