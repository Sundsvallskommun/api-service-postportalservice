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

	private static final String VALID_ENTRY_REGEX = "^\\d{8}-?\\d{4}$";

	/**
	 * Validates that a given CSV file has a header line with "Personnummer" and that each data row contains exactly 12
	 * digits. Returns a map with the counts of each unique personal identity number found in the CSV.
	 *
	 * @param  csvFile the CSV file to validate
	 * @return         a map with personal identity numbers as keys and their counts as values
	 */
	public static Map<String, Integer> validateCSV(final MultipartFile csvFile) {
		Map<String, Integer> counts = new LinkedHashMap<>();

		String line;
		boolean headerRead = false;

		try (var reader = new BufferedReader(new InputStreamReader(csvFile.getInputStream(), StandardCharsets.UTF_8))) {
			while ((line = reader.readLine()) != null) {
				var trimmed = line.trim();

				if (!headerRead && "Personnummer".equals(trimmed)) {
					headerRead = true;
					// Skip header line
					continue;
				} else {
					// Even if not the correct header (it might be a legalId), we consider it read after the first line
					// If the header is something else than "Personnummer", we still process it, if it's not a valid entry it will be caught
					// by the validation below
					headerRead = true;
				}

				// Validate that the line contains exactly 12 digits, with an optional hyphen between digit 8 and 9
				if (!trimmed.isEmpty() && !trimmed.matches(VALID_ENTRY_REGEX)) {
					throw Problem.valueOf(BAD_REQUEST, "Invalid CSV format. CSV may contain an optional 'Personnummer' header. Each data row must contain 12 digits, an optional hyphen between digit 8 and 9 are acceptable. Invalid entry: " + trimmed);
				}

				if (!trimmed.isEmpty()) {
					counts.merge(trimmed.replace("-", ""), 1, Integer::sum);
				}
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
