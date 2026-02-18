package se.sundsvall.postportalservice.integration.db.dao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

/**
 * DepartmentRepository tests.
 *
 * @see "/src/test/resources/db/script/testdata.sql for data setup"
 */
@Sql(scripts = {
	"/db/script/testdata.sql"
})
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
class DepartmentRepositoryTest {

	@Autowired
	private DepartmentRepository departmentRepository;

	@Test
	void findByOrganizationId() {
		final var result = departmentRepository.findByOrganizationId("3");

		assertThat(result).isPresent().hasValueSatisfying(department -> {
			assertThat(department.getId()).isEqualTo("e3e146fb-aac9-467c-a19a-c90ee82caed4");
			assertThat(department.getName()).isEqualTo("IT-avdelningen");
		});
	}

	@Test
	void findByOrganizationIdNoMatch() {
		final var result = departmentRepository.findByOrganizationId("666");

		assertThat(result).isNotPresent();
	}
}
