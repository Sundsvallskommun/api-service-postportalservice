package se.sundsvall.postportalservice.integration.db.dao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

/**
 * UserRepository tests.
 *
 * @see "/src/test/resources/db/script/testdata.sql for data setup"
 */
@Sql(scripts = {
	"/db/script/testdata.sql"
})
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
class UserRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	@ParameterizedTest
	@ValueSource(strings = {
		"usEr1", "user1", "USER1"
	})
	void findByUsernameIgnoreCase(String username) {
		final var result = userRepository.findByUsernameIgnoreCase(username);

		assertThat(result).isPresent().hasValueSatisfying(user -> {
			assertThat(user.getId()).isEqualTo("4724b00c-1b1a-490d-ae43-9fb6237c6171");
			assertThat(user.getUsername()).isEqualTo("user1");
		});
	}

	@Test
	void findByUsernameIgnoreCaseNoMatch() {
		final var result = userRepository.findByUsernameIgnoreCase("user3");

		assertThat(result).isNotPresent();
	}
}
