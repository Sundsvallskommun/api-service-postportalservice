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
}
