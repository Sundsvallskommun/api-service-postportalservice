package se.sundsvall.postportalservice.integration.db.dao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.postportalservice.integration.db.SigningEntity;

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
	void findByProviderCaseId() {
		assertThat(signingRepository.findByProviderCaseId("provider-case-1"))
			.isPresent()
			.hasValueSatisfying(signing -> {
				assertThat(signing.getId()).isEqualTo("7c9e6679-7425-40de-944b-e07fc1f90ae7");
				assertThat(signing.getMessageId()).isEqualTo("b2cd4957-228f-46f0-a263-d4eae2eb5f52");
				assertThat(signing.getAttachmentId()).isEqualTo("5f6757a4-f0c7-43c0-83b1-78cafb5b7291");
				assertThat(signing.getProvider()).isEqualTo("comfact");
				assertThat(signing.getStatus()).isEqualTo("INVANTAR_SIGNERING");
			});
	}

	@Test
	void findByProviderCaseIdNoMatch() {
		assertThat(signingRepository.findByProviderCaseId("does-not-exist")).isEmpty();
	}

	@Test
	void save() {
		final var saved = signingRepository.save(SigningEntity.create()
			.withMessageId("b2cd4957-228f-46f0-a263-d4eae2eb5f52")
			.withProviderCaseId("provider-case-2")
			.withProvider("comfact")
			.withStatus("INITIERAT"));

		assertThat(saved.getId()).isNotBlank();
		assertThat(saved.getCreated()).isNotNull();
	}
}
