package se.sundsvall.postportalservice.util;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

public final class LegalIdUtil {

	// Using strict resolver style to ensure dates like 20230230 are considered invalid
	private static final DateTimeFormatter BIRTH_DATE_FORMATTER = DateTimeFormatter.ofPattern("uuuuMMdd")
		.withResolverStyle(ResolverStyle.STRICT);
	private static final int PERSON_ID_LENGTH = 12;
	private static final int ADULT_AGE = 18;

	private LegalIdUtil() {}

	/**
	 * Check that the person is an adult (18 years or older).
	 *
	 * @param  legalId the person's legal Id (12 digits: YYYYMMDDXXXX)
	 * @return         true if the person is an adult (18 years or older), false otherwise
	 */
	public static boolean isAnAdult(final String legalId) {
		// Sanity check if we for some reason would receive a null or malformed personId
		if (legalId == null || legalId.length() != PERSON_ID_LENGTH) {
			return false;
		}

		try {
			// Extract the year, month and date portion and parse it to a LocalDate
			var birthDate = LocalDate.parse(legalId.substring(0, 8), BIRTH_DATE_FORMATTER);

			// Ensure the birthdate is not in the future
			if (birthDate.isAfter(LocalDate.now())) {
				return false;
			}

			var age = Period.between(birthDate, LocalDate.now()).getYears();

			return age >= ADULT_AGE;
		} catch (final Exception _) {
			// If parsing fails or any other exception occurs, treat as invalid
			return false;
		}
	}
}
