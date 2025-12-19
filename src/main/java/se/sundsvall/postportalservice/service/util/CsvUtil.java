package se.sundsvall.postportalservice.service.util;

import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.web.multipart.MultipartFile;
import org.zalando.problem.Problem;

public final class CsvUtil {

	private CsvUtil() {}

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

				// Skip empty lines
				if (trimmed.isEmpty()) {
					continue;
				}

				if (!headerRead) {
					// Header must be exactly "Personnummer"
					if (!"Personnummer".equals(trimmed)) {
						throw Problem.valueOf(BAD_REQUEST, "CSV header is invalid. Expected 'Personnummer' but found: " + trimmed);
					}
					headerRead = true;
					continue;
				}

				// Validate that the line contains exactly 12 digits, with an optional hyphen between digit 8 and 9
				if (!trimmed.matches("^\\d{8}-?\\d{4}$")) {
					throw Problem.valueOf(BAD_REQUEST, "Invalid CSV format. Each data row must contain 12 digits, an optional hyphen between digit 8 and 9 are acceptable. Invalid entry: " + trimmed);
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
				var legalId = columns[0].trim();

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
