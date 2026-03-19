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

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

public final class CsvUtil {

	private CsvUtil() {}

	private static final Set<String> VALID_HEADERS = Set.of("Phonenumber", "Telefonnummer", "Mobilnummer");
	private static final String VALID_PHONE_NUMBER_REGEX = "^\\+[1-9][\\d]{3,14}$";
	private static final String DEFAULT_COUNTRY_CODE = "+46";

	private static final String VALID_LEGAL_ID_REGEX = "^\\d{8}-?\\d{4}$";

	public record SmsCsvValidationResult(
		Map<String, Integer> validEntries,
		Set<String> invalidEntries) {}

	public static SmsCsvValidationResult validateSmsCsv(final MultipartFile csvFile) {
		Map<String, Integer> validEntries = new LinkedHashMap<>();
		Set<String> invalidEntries = new HashSet<>();
		boolean headerRead = false;

		try (var reader = new BufferedReader(new InputStreamReader(csvFile.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				var trimmed = line.trim();

				if (!headerRead) {
					headerRead = true;
					if (VALID_HEADERS.contains(trimmed)) {
						continue;
					}
				}

				if (trimmed.isEmpty()) {
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
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, "Error reading CSV file: " + e.getMessage());
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
	 * Validates that a given CSV file has a header line with "Personnummer" and that each data row contains exactly 12
	 * digits. Returns a map with the counts of each unique personal identity number found in the CSV.
	 *
	 * @param  csvFile the CSV file to validate
	 * @return         a map with personal identity numbers as keys and their counts as values
	 */
	public static Map<String, Integer> validateLetterCsv(final MultipartFile csvFile) {
		Map<String, Integer> counts = new LinkedHashMap<>();
		boolean headerRead = false;

		try (var reader = new BufferedReader(new InputStreamReader(csvFile.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				var trimmed = line.trim();

				if (!headerRead) {
					headerRead = true;
					if ("Personnummer".equals(trimmed)) {
						// If the header was "Personnummer", we skip it and process the next line.
						continue;
					}
				}
				if (trimmed.isEmpty()) {
					// If the line is empty, skip and process the next line.
					continue;
				}

				// Validate that the line contains exactly 12 digits, with an optional hyphen between digit 8 and 9
				if (!trimmed.matches(VALID_LEGAL_ID_REGEX)) {
					throw Problem.valueOf(BAD_REQUEST, "Invalid CSV format. CSV may contain an optional 'Personnummer' header. Each data row must contain 12 digits, an optional hyphen between digit 8 and 9 are acceptable. Invalid entry: " + trimmed);
				}

				counts.merge(trimmed.replace("-", ""), 1, Integer::sum);

			}
			return counts;

		} catch (IOException e) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, "Error reading CSV file: " + e.getMessage());
		}
	}

	/**
	 * Parses a CSV file containing legal IDs and returns a set of unique legal IDs.
	 *
	 * @param  csvFile the CSV file to parse
	 * @return         a set of unique legal IDs
	 */
	public static Set<String> parseCsvToLegalIds(final MultipartFile csvFile) {
		Set<String> legalIds = new HashSet<>();

		try (var reader = new BufferedReader(new InputStreamReader(csvFile.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {

				if (line.trim().isEmpty() || line.trim().startsWith("Personnummer")) {
					continue;
				}

				var columns = line.split(";");
				var legalId = columns[0].trim().replace("-", "");

				if (!legalId.isEmpty()) {
					legalIds.add(legalId);
				}
			}
		} catch (IOException e) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, "Error reading CSV file: " + e.getMessage());
		}

		return legalIds;
	}
}
