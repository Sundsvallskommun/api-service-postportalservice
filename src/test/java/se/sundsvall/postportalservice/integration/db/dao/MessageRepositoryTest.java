package se.sundsvall.postportalservice.integration.db.dao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.postportalservice.integration.db.MessageEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;
import static se.sundsvall.postportalservice.integration.db.converter.MessageType.DIGITAL_REGISTERED_LETTER;
import static se.sundsvall.postportalservice.integration.db.converter.MessageType.SNAIL_MAIL;

/**
 * MessageRepository tests.
 *
 * @see "/src/test/resources/db/script/testdata.sql for data setup"
 */
@Sql(scripts = {
	"/db/script/testdata.sql"
})
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
class MessageRepositoryTest {

	@Autowired
	private MessageRepository messageRepository;

	@ParameterizedTest
	@ValueSource(strings = {
		"usEr1", "user1", "USER1"
	})
	void findAllByMunicipalityIdAndUserUsernameIgnoreCase(String username) {
		final var result = messageRepository.findAllByMunicipalityIdAndUserUsernameIgnoreCase("2281", username, PageRequest.ofSize(100));

		assertThat(result)
			.hasSize(4)
			.extracting(MessageEntity::getId)
			.containsExactlyInAnyOrder(
				"ab4cdf50-b854-48f8-a061-1e89f9792c9a",
				"5ab7aa30-b7fc-404a-89a3-f30fa5667979",
				"b2cd4957-228f-46f0-a263-d4eae2eb5f52",
				"4972e098-21b8-4fda-9a0b-4d1b1377f7e4");
	}

	@Test
	void findAllByMunicipalityIdAndUserUsernameIgnoreCaseNoMatch() {
		assertThat(messageRepository.findAllByMunicipalityIdAndUserUsernameIgnoreCase("2281", "user3", PageRequest.ofSize(100))).isEmpty(); // Username does not match
		assertThat(messageRepository.findAllByMunicipalityIdAndUserUsernameIgnoreCase("2262", "user1", PageRequest.ofSize(100))).isEmpty(); // Municipality does not match
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"usEr1", "user1", "USER1"
	})
	void findByMunicipalityIdAndIdAndUserUsernameIgnoreCase(String username) {
		assertThat(messageRepository.findByMunicipalityIdAndIdAndUserUsernameIgnoreCase("2281", "ab4cdf50-b854-48f8-a061-1e89f9792c9a", username))
			.isPresent()
			.hasValueSatisfying(messageEntity -> {
				assertThat(messageEntity.getId()).isEqualTo("ab4cdf50-b854-48f8-a061-1e89f9792c9a");
			});
	}

	@Test
	void findByMunicipalityIdAndIdAndUserUsernameIgnoreCaseNoMatch() {
		assertThat(messageRepository.findByMunicipalityIdAndIdAndUserUsernameIgnoreCase("2281", "ab4cdf50-b854-48f8-a061-1e89f9792c9a", "user3")).isEmpty(); // Username does not match
		assertThat(messageRepository.findByMunicipalityIdAndIdAndUserUsernameIgnoreCase("2281", "35cf926b-a9d8-47a7-8b82-7351c94d84bc", "user1")).isEmpty(); // Message id does not match
		assertThat(messageRepository.findByMunicipalityIdAndIdAndUserUsernameIgnoreCase("2262", "ab4cdf50-b854-48f8-a061-1e89f9792c9a", "user1")).isEmpty(); // Municipality does not match
	}

	@Test
	void findByIdAndMessageType() {
		assertThat(messageRepository.findByIdAndMessageType("1decdead-52b8-42d9-aa62-5ef08c4a701e", DIGITAL_REGISTERED_LETTER))
			.isPresent()
			.hasValueSatisfying(messageEntity -> {
				assertThat(messageEntity.getId()).isEqualTo("1decdead-52b8-42d9-aa62-5ef08c4a701e");
			});
	}

	@Test
	void findByIdAndMessageTypeNoMatch() {
		assertThat(messageRepository.findByIdAndMessageType("11d7486b-5da0-4e73-92ec-7d77a3660f55", DIGITAL_REGISTERED_LETTER)).isEmpty(); // Message id does not match
		assertThat(messageRepository.findByIdAndMessageType("1decdead-52b8-42d9-aa62-5ef08c4a701e", SNAIL_MAIL)).isEmpty(); // Message type does not match
	}
}
