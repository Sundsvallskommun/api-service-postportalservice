package se.sundsvall.postportalservice.service.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.postportalservice.util.LegalIdUtil;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

public final class CsvUtil {

	private CsvUtil() {}

	private static final String COULD_NOT_READ_CSV_FILE = "Could not read CSV file: %s";
	private static final Set<String> VALID_HEADERS = Set.of("Phonenumber", "Telefonnummer", "Mobilnummer");
	private static final Set<String> VALID_LETTER_HEADERS = Set.of("Personnummer", "Identitetsnummer");
	private static final String VALID_PHONE_NUMBER_REGEX = "^\\+46\\d{9}$";
	private static final String DEFAULT_COUNTRY_CODE = "+46";
	// Accepts either 12-digit personnummer (with optional hyphen between digits 8 and 9) or 10-digit orgnr (with optional
	// hyphen between digits 6 and 7)
	private static final String VALID_LETTER_ID_REGEX = "^(\\d{8}-?\\d{4}|\\d{6}-?\\d{4})$";

	public record SmsCsvValidationResult(
		Map<String, Integer> validEntries,
		Set<String> invalidEntries) {
	}

	public record LetterCsvParseResult(
		Map<String, Integer> personnummer,
		Map<String, Integer> orgnummer) {
	}

	public static SmsCsvValidationResult validateSmsCsv(final MultipartFile csvFile) {
		Map<String, Integer> validEntries = new LinkedHashMap<>();
		Set<String> invalidEntries = new HashSet<>();
		boolean headerRead = false;

		try (var reader = new BufferedReader(new InputStreamReader(csvFile.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				var trimmed = line.trim();

				var isHeader = false;
				if (!headerRead) {
					headerRead = true;
					isHeader = VALID_HEADERS.contains(trimmed);
				}

				if (isHeader || trimmed.isEmpty()) {
					continue;
				}

				var normalized = normalizeToMsisdn(trimmed);
				if (!normalized.matches(VALID_PHONE_NUMBER_REGEX)) {
					invalidEntries.add(trimmed);
				} else {
					validEntries.merge(normalized, 1, Integer::sum);
				}
			}
			return new SmsCsvValidationResult(validEntries, invalidEntries);

		} catch (IOException e) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, COULD_NOT_READ_CSV_FILE.formatted(e.getMessage()));
		}
	}

	private static String normalizeToMsisdn(String input) {
		// Strip all whitespace and hyphens
		var stripped = input.replaceAll("[\\s-]", "");

		// Replace the leading 0 with country code (e.g., 0701740605 -> +46701740605)
		if (stripped.startsWith("0")) {
			stripped = DEFAULT_COUNTRY_CODE + stripped.substring(1);
		}

		return stripped;
	}

	/**
	 * Validates a letter CSV file and classifies each row as personnummer (12 digits) or organisationsnummer (10 digits,
	 * 3rd
	 * digit >= 2). The header line ("Personnummer" or "Identitetsnummer") is optional. Hyphens in numbers are accepted and
	 * stripped. Sole proprietors (10-digit IDs with 3rd digit < 2) are rejected with a clear message asking the sender to
	 * submit them as 12-digit personnummer.
	 *
	 * @param  csvFile the CSV file to validate
	 * @return         a {@link LetterCsvParseResult} with counts of each unique number, bucketed by type
	 */
	public static LetterCsvParseResult parseLetterCsv(final MultipartFile csvFile) {
		final Map<String, Integer> personnummer = new LinkedHashMap<>();
		final Map<String, Integer> orgnummer = new LinkedHashMap<>();
		boolean headerRead = false;

		try (var reader = new BufferedReader(new InputStreamReader(csvFile.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				var trimmed = line.trim();

				var isHeader = false;
				if (!headerRead) {
					headerRead = true;
					isHeader = VALID_LETTER_HEADERS.contains(trimmed);
				}

				if (isHeader || trimmed.isEmpty()) {
					continue;
				}

				if (!trimmed.matches(VALID_LETTER_ID_REGEX)) {
					throw Problem.valueOf(BAD_REQUEST,
						"Invalid CSV format. CSV may contain an optional 'Personnummer' or 'Identitetsnummer' header. Each data row must contain either a 12-digit personnummer (optional hyphen between digit 8 and 9) or a 10-digit organisationsnummer (optional hyphen between digit 6 and 7). Invalid entry: "
							+ trimmed);
				}

				final var normalized = trimmed.replace("-", "");
				if (LegalIdUtil.isPrivateLegalId(normalized)) {
					personnummer.merge(normalized, 1, Integer::sum);
				} else if (LegalIdUtil.isOrgNumber(normalized)) {
					orgnummer.merge(normalized, 1, Integer::sum);
				} else {
					// 10-digit IDs with 3rd digit < 2 are sole proprietors (enskilda firmor) using a personnummer as their
					// organisationsnummer. The Party PRIVATE batch endpoint expects 12-digit personnummer, so we reject these
					// at the boundary.
					throw Problem.valueOf(BAD_REQUEST, "Invalid CSV row '" + trimmed + "': sole proprietors (enskilda firmor) must be submitted as a 12-digit personnummer.");
				}
			}
			return new LetterCsvParseResult(personnummer, orgnummer);

		} catch (IOException e) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, COULD_NOT_READ_CSV_FILE.formatted(e.getMessage()));
		}
	}

	/**
	 * Parses a CSV file containing legal IDs and returns a set of unique legal IDs (personnummer + orgnummer combined).
	 * Backward-compatible helper used by callers that don't need per-type counts.
	 *
	 * @param  csvFile the CSV file to parse
	 * @return         a set of unique legal IDs (hyphens stripped)
	 */
	public static Set<String> parseCsvToLegalIds(final MultipartFile csvFile) {
		final Set<String> legalIds = new HashSet<>();
		final var parsed = parseLetterCsv(csvFile);
		legalIds.addAll(parsed.personnummer().keySet());
		legalIds.addAll(parsed.orgnummer().keySet());
		return legalIds;
	}
}
