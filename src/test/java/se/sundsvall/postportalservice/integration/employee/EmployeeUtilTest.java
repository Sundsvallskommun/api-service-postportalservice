package se.sundsvall.postportalservice.integration.employee;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmployeeUtilTest {

	@Test
	void parseOrganizationString() {
		var organizationString = "2|28|Kommunstyrelsekontoret¤3|440|KSK Avdelningar¤4|2835|KSK AVD Digital Transformation¤5|2834|KSK AVD Digitalisering IT stab¤6|2836|KSK AVD Digitalisering IT stab";

		var result = EmployeeUtil.parseOrganizationString(organizationString);

		assertThat(result).isPresent();
		assertThat(result.get().identifier()).isEqualTo("28");
		assertThat(result.get().name()).isEqualTo("Kommunstyrelsekontoret");
	}

	@Test
	void parseOrganizationStringWithNull() {
		var result = EmployeeUtil.parseOrganizationString(null);

		assertThat(result).isEmpty();
	}
}
