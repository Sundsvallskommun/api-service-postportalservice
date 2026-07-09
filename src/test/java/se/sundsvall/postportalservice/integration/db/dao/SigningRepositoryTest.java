package se.sundsvall.postportalservice.integration.db.dao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

/**
 * SigningRepository tests.
 *
 * @see "/src/integration-test/resources/db/scripts/testdata.sql for data setup"
 */
@Sql(scripts = {
	"/db/scripts/testdata.sql"
})
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
class SigningRepositoryTest {

	private static final String MESSAGE_ID = "1decdead-52b8-42d9-aa62-5ef08c4a701e";

	@Autowired
	private SigningRepository signingRepository;

	@Test
	void findByIdLoadsRelationships() {
		assertThat(signingRepository.findById("7c9e6679-7425-40de-944b-e07fc1f90ae7"))
			.isPresent()
			.hasValueSatisfying(signing -> {
				assertThat(signing.getMessage().getId()).isEqualTo("b2cd4957-228f-46f0-a263-d4eae2eb5f52");
				assertThat(signing.getAttachment().getId()).isEqualTo("5f6757a4-f0c7-43c0-83b1-78cafb5b7291");
				assertThat(signing.getProvider()).isEqualTo("comfact");
				assertThat(signing.getStatus()).isEqualTo("INVANTAR_SIGNERING");
			});
	}

	@Test
	void findByMessageId() {
		final var result = signingRepository.findByMessageId(MESSAGE_ID);

		assertThat(result).isPresent().hasValueSatisfying(signing -> {
			assertThat(signing.getId()).isEqualTo("a1b2c3d4-0000-4000-8000-000000000001");
			assertThat(signing.getMessage().getId()).isEqualTo(MESSAGE_ID);
			assertThat(signing.getProviderCaseId()).isEqualTo("comfact-case-1");
			assertThat(signing.getProvider()).isEqualTo("comfact");
			assertThat(signing.getStatus()).isEqualTo("INVANTAR_SIGNERING");
		});
	}

	@Test
	void findByMessageIdNoMatch() {
		final var result = signingRepository.findByMessageId("00000000-0000-0000-0000-000000000000");

		assertThat(result).isEmpty();
	}
}
