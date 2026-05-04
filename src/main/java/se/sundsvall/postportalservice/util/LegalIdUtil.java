package se.sundsvall.postportalservice.util;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

public final class LegalIdUtil {

	// Using strict resolver style to ensure dates like 20230230 are considered invalid
	private static final DateTimeFormatter BIRTH_DATE_FORMATTER = DateTimeFormatter.ofPattern("uuuuMMdd")
		.withResolverStyle(ResolverStyle.STRICT);
	private static final int ORG_NUMBER_LENGTH = 10;
	private static final int ORG_NUMBER_PREFIXED_WITH_16_LENGTH = 12;
	private static final int PERSON_ID_LENGTH = 12;
	private static final int ADULT_AGE = 18;

	private LegalIdUtil() {}

	/**
	 * Check whether the given id is a Swedish organization number.
	 *
	 * Accepts both 10-digit and 12-digit "16"-prefixed forms. Returns true only when the third digit (index 2 after
	 * stripping any "16" prefix) is >= 2 — this disambiguates org numbers from personal identity numbers (where the third
	 * digit is the first month digit, 0 or 1).
	 *
	 * @param  legalId the id to check
	 * @return         true if the id classifies as an organization number, false otherwise
	 */
	public static boolean isOrgNumber(final String legalId) {
		if (legalId == null) {
			return false;
		}

		final var length = legalId.length();
		if (length != ORG_NUMBER_LENGTH && length != ORG_NUMBER_PREFIXED_WITH_16_LENGTH) {
			return false;
		}

		final var stripped = length == ORG_NUMBER_PREFIXED_WITH_16_LENGTH ? legalId.substring(2) : legalId;

		try {
			final var monthPart = Integer.parseInt(stripped.substring(2, 3));
			return monthPart >= 2;
		} catch (final NumberFormatException _) {
			return false;
		}
	}

	/**
	 * Check whether the given id is a 12-digit Swedish personal identity number (personnummer).
	 *
	 * Returns true for 12-digit ids that are NOT classified as a 12-digit "16"-prefixed organization number.
	 *
	 * @param  legalId the id to check
	 * @return         true if the id classifies as a personnummer, false otherwise
	 */
	public static boolean isPrivateLegalId(final String legalId) {
		return legalId != null && legalId.length() == PERSON_ID_LENGTH && !isOrgNumber(legalId);
	}

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
