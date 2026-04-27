package se.sundsvall.postportalservice.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class LegalIdUtilTest {

	@ParameterizedTest(name = "{0}")
	@MethodSource("legalIdProvider")
	void testIsAnAdult(String testName, String personId, boolean expectedResult) {
		assertThat(LegalIdUtil.isAnAdult(personId)).isEqualTo(expectedResult);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("orgNumberProvider")
	void testIsOrgNumber(String testName, String legalId, boolean expectedResult) {
		assertThat(LegalIdUtil.isOrgNumber(legalId)).isEqualTo(expectedResult);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("privateLegalIdProvider")
	void testIsPrivateLegalId(String testName, String legalId, boolean expectedResult) {
		assertThat(LegalIdUtil.isPrivateLegalId(legalId)).isEqualTo(expectedResult);
	}

	public static Stream<Arguments> orgNumberProvider() {
		return Stream.of(
			// Valid org numbers (3rd digit >= 2)
			Arguments.of("10-digit org number, 3rd digit 2", "5523456789", true),
			Arguments.of("10-digit org number, 3rd digit 9", "5593456789", true),
			Arguments.of("12-digit '16'-prefixed org number", "165523456789", true),

			// Sole proprietors (enskilda firmor) - 3rd digit < 2 means NOT classified as org number
			Arguments.of("10-digit enskild firma, 3rd digit 0", "5503456789", false),
			Arguments.of("10-digit enskild firma, 3rd digit 1", "5513456789", false),

			// Personal numbers (3rd digit < 2 because it's the first month digit)
			Arguments.of("12-digit personnummer (month 01)", "198601010000", false),
			Arguments.of("12-digit personnummer (month 12)", "199212310000", false),

			// Edge cases
			Arguments.of("Null", null, false),
			Arguments.of("Empty", "", false),
			Arguments.of("Too short (5)", "12345", false),
			Arguments.of("Too long (13)", "1234567890123", false),
			Arguments.of("Wrong length (11)", "12345678901", false),
			Arguments.of("Non-numeric 3rd char", "12A2345678", false));
	}

	public static Stream<Arguments> privateLegalIdProvider() {
		return Stream.of(
			// Valid personnummer
			Arguments.of("12-digit personnummer (month 01)", "198601010000", true),
			Arguments.of("12-digit personnummer (month 12)", "199212310000", true),

			// Org numbers should NOT be private
			Arguments.of("10-digit org number", "5523456789", false),
			Arguments.of("12-digit '16'-prefixed org number", "165523456789", false),

			// 12-digit '16'-prefixed value where stripped has 3rd digit < 2 — neither org nor really private; classify as private
			// (has 12 digits)
			Arguments.of("12-digit '16'-prefixed but stripped 3rd digit 1", "165512345678", true),

			// Edge cases
			Arguments.of("Null", null, false),
			Arguments.of("Empty", "", false),
			Arguments.of("10-digit (not 12)", "1986010100", false),
			Arguments.of("Too short", "19860101", false));
	}

	public static Stream<Arguments> legalIdProvider() {
		final var today = LocalDate.now();
		final var formatter = DateTimeFormatter.ofPattern("uuuuMMdd");  // Using strict mode in the actual implementation

		// Dynamic dates
		final var exactly18Years = today.minusYears(18);
		final var turned18Yesterday = exactly18Years.minusDays(1);
		final var turns18Tomorrow = exactly18Years.plusDays(1);
		final var over18Years = today.minusYears(20);
		final var under18Years = today.minusYears(15);
		final var notYetBorn = today.plusYears(5);

		return Stream.of(
			// Valid adults (18 years or older)
			Arguments.of("Adult 20 years old", over18Years.format(formatter) + "0000", true),
			Arguments.of("Person exactly 18 years old today", exactly18Years.format(formatter) + "0000", true),
			Arguments.of("Person turned 18 yesterday", turned18Yesterday.format(formatter) + "0000", true),

			// Valid minors (under 18)
			Arguments.of("Person turns 18 tomorrow (17 years old)", turns18Tomorrow.format(formatter) + "0000", false),
			Arguments.of("Minor 15 years old", under18Years.format(formatter) + "0000", false),

			// Edge cases
			Arguments.of("Null person ID", null, false),
			Arguments.of("Empty string", "", false),
			Arguments.of("Too short", "19856", false),
			Arguments.of("Too long", "1986010100001", false),

			// Invalid dates that will throw DateTimeParseException
			Arguments.of("Invalid day (32nd)", "200713320000", false),
			Arguments.of("Invalid month (00)", "200700010000", false),
			Arguments.of("Invalid date (Feb 30)", "200702300000", false),
			Arguments.of("Non-numeric characters", "ABCD12340000", false),

			// Future dates
			Arguments.of("Future date (+5 years)", notYetBorn.format(formatter) + "0000", false));
	}
}
