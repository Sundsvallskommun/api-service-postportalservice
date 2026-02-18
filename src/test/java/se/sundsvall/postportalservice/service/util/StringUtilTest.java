package se.sundsvall.postportalservice.service.util;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class StringUtilTest {

	@ParameterizedTest(name = "{0}")
	@MethodSource("calculateFullNameProvider")
	void calculateFullName(String testName, String firstName, String lastName, String expectedResult) {
		assertThat(StringUtil.calculateFullName(firstName, lastName)).isEqualTo(expectedResult);
	}

	private static Stream<Arguments> calculateFullNameProvider() {
		return Stream.of(
			Arguments.of("No values for either first or last name", null, null, null),
			Arguments.of("Only value for first name", "Trinity", null, "Trinity"),
			Arguments.of("Only value for last name", null, "Ballard", "Ballard"),
			Arguments.of("Values for both first and last name", "Agent", "Smith", "Agent Smith"),
			Arguments.of("Values for both simple first and complex last name", "Deus", "Ex Machina", "Deus Ex Machina"),
			Arguments.of("Values for both complex first and simple last name", "Councillor Roland", "Hamann", "Councillor Roland Hamann"),
			Arguments.of("Test of trimming values for both first and last name", " Shimada ", " Tyndall ", "Shimada Tyndall"),
			Arguments.of("Test of trimming values for both complex first and complex last name", " Captain Soren ", " Bane Rhineheart ", "Captain Soren Bane Rhineheart"));
	}
}
