package se.sundsvall.postportalservice.service.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.test.annotation.resource.Load;
import se.sundsvall.dept44.test.extension.ResourceLoaderExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(ResourceLoaderExtension.class)
class CsvUtilTest {

	@Test
	void parseCsvToLegalIds(@Load(value = "/testfile/legalIds.csv") final String csv) throws IOException {
		var multipartFileMock = Mockito.mock(MultipartFile.class);

		when(multipartFileMock.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
		when(multipartFileMock.getContentType()).thenReturn("text/csv");
		when(multipartFileMock.getOriginalFilename()).thenReturn("legalIds.csv");

		var result = CsvUtil.parseCsvToLegalIds(multipartFileMock);

		assertThat(result).containsExactlyInAnyOrder(
			"201901012391", "201901022382", "201901032399",
			"201901042380", "201901052397", "201901062388");
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"/testfile/legalIds-without-header.csv", "/testfile/legalIds.csv"
	})
	void parseLetterCsvToPersonnummer(final String resourcePath) throws IOException {
		var csv = new String(getClass().getResourceAsStream(resourcePath).readAllBytes(), StandardCharsets.UTF_8);
		var multipartFileMock = Mockito.mock(MultipartFile.class);

		when(multipartFileMock.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
		when(multipartFileMock.getContentType()).thenReturn("text/csv");
		when(multipartFileMock.getOriginalFilename()).thenReturn("legalIds.csv");

		var result = CsvUtil.parseLetterCsv(multipartFileMock);

		assertCsvContent(result.personnummer());
		assertThat(result.orgnummer()).isEmpty();
	}

	private void assertCsvContent(final Map<String, Integer> values) {
		assertThat(values).containsExactlyInAnyOrderEntriesOf(
			java.util.Map.of(
				"201901012391", 1,
				"201901022382", 1,
				"201901032399", 1,
				"201901042380", 1,
				"201901052397", 1,
				"201901062388", 1));
	}

	@Test
	void parseLetterCsvWithDuplicates(@Load(value = "/testfile/legalIds-duplicates.csv") final String csv) throws IOException {
		var multipartFileMock = Mockito.mock(MultipartFile.class);

		when(multipartFileMock.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
		when(multipartFileMock.getContentType()).thenReturn("text/csv");
		when(multipartFileMock.getOriginalFilename()).thenReturn("legalIds.csv");

		var result = CsvUtil.parseLetterCsv(multipartFileMock);

		assertThat(result.personnummer()).containsExactlyInAnyOrderEntriesOf(
			java.util.Map.of(
				"201901012391", 2,
				"201901012392", 2));
		assertThat(result.orgnummer()).isEmpty();
	}

	@Test
	void parseLetterCsvWithMixedTypes() throws IOException {
		final var csv = "Identitetsnummer\n201901012391\n5523456789\n";
		var multipartFileMock = Mockito.mock(MultipartFile.class);
		when(multipartFileMock.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

		var result = CsvUtil.parseLetterCsv(multipartFileMock);

		assertThat(result.personnummer()).containsExactlyInAnyOrderEntriesOf(java.util.Map.of("201901012391", 1));
		assertThat(result.orgnummer()).containsExactlyInAnyOrderEntriesOf(java.util.Map.of("5523456789", 1));
	}

	@Test
	void parseLetterCsvAcceptsLegacyPersonnummerHeader() throws IOException {
		final var csv = "Personnummer\n201901012391\n";
		var multipartFileMock = Mockito.mock(MultipartFile.class);
		when(multipartFileMock.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

		var result = CsvUtil.parseLetterCsv(multipartFileMock);

		assertThat(result.personnummer()).containsKey("201901012391");
	}

	@Test
	void parseLetterCsvRejectsTenDigitEnskildFirma() throws IOException {
		final var csv = "Identitetsnummer\n5513456789\n";
		var multipartFileMock = Mockito.mock(MultipartFile.class);
		when(multipartFileMock.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

		assertThatThrownBy(() -> CsvUtil.parseLetterCsv(multipartFileMock))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("sole proprietors")
			.hasMessageContaining("12-digit personnummer");
	}

	@Test
	void parseLetterCsvRejectsMalformedRow() throws IOException {
		final var csv = "Identitetsnummer\nABC\n";
		var multipartFileMock = Mockito.mock(MultipartFile.class);
		when(multipartFileMock.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

		assertThatThrownBy(() -> CsvUtil.parseLetterCsv(multipartFileMock))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Invalid CSV format");
	}

	@Test
	void parseLetterCsvAcceptsHyphenatedNumbers() throws IOException {
		final var csv = "Identitetsnummer\n20190101-2391\n552345-6789\n";
		var multipartFileMock = Mockito.mock(MultipartFile.class);
		when(multipartFileMock.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

		var result = CsvUtil.parseLetterCsv(multipartFileMock);

		assertThat(result.personnummer()).containsKey("201901012391");
		assertThat(result.orgnummer()).containsKey("5523456789");
	}

	@Test
	void parseCsvToLegalId_throws() throws IOException {
		var multipartFileMock = Mockito.mock(MultipartFile.class);

		var customExceptionMessage = "Test exception";
		when(multipartFileMock.getInputStream()).thenThrow(new IOException(customExceptionMessage));

		assertThatThrownBy(() -> CsvUtil.parseCsvToLegalIds(multipartFileMock))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Internal Server Error: Could not read CSV file: %s".formatted(customExceptionMessage));
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"/testfile/phoneNumbers.csv", "/testfile/phoneNumbers-without-header.csv"
	})
	void validateSmsCsv(final String resourcePath) throws IOException {
		var csv = new String(getClass().getResourceAsStream(resourcePath).readAllBytes(), StandardCharsets.UTF_8);
		var multipartFileMock = Mockito.mock(MultipartFile.class);
		when(multipartFileMock.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

		var result = CsvUtil.validateSmsCsv(multipartFileMock);

		assertThat(result.validEntries()).containsExactlyInAnyOrderEntriesOf(Map.of(
			"+46701740610", 1,
			"+46701740620", 1,
			"+46701740630", 1));
		assertThat(result.invalidEntries()).isEmpty();
	}

	@Test
	void validateSmsCsvWithDuplicates(@Load(value = "/testfile/phoneNumbers-duplicates.csv") final String csv) throws IOException {
		var multipartFileMock = Mockito.mock(MultipartFile.class);
		when(multipartFileMock.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

		var result = CsvUtil.validateSmsCsv(multipartFileMock);

		assertThat(result.validEntries()).containsExactlyInAnyOrderEntriesOf(Map.of(
			"+46701740610", 2,
			"+46701740620", 3));
		assertThat(result.invalidEntries()).isEmpty();
	}

	@Test
	void validateSmsCsvWithMixedValidAndInvalid(@Load(value = "/testfile/phoneNumbers-mixed.csv") final String csv) throws IOException {
		var multipartFileMock = Mockito.mock(MultipartFile.class);
		when(multipartFileMock.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

		var result = CsvUtil.validateSmsCsv(multipartFileMock);

		assertThat(result.validEntries()).containsExactlyInAnyOrderEntriesOf(Map.of(
			"+46701740610", 1,
			"+46701740620", 1));
		assertThat(result.invalidEntries()).containsExactlyInAnyOrder("notanumber", "abc123");
	}

	@Test
	void validateSmsCsvAllInvalid(@Load(value = "/testfile/phoneNumbers-all-invalid.csv") final String csv) throws IOException {
		var multipartFileMock = Mockito.mock(MultipartFile.class);
		when(multipartFileMock.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

		var result = CsvUtil.validateSmsCsv(multipartFileMock);

		assertThat(result.validEntries()).isEmpty();
		assertThat(result.invalidEntries()).containsExactlyInAnyOrder("notanumber", "abc123", "12345");
	}

	@Test
	void validateSmsCsvRejectsInternationalNumbers(@Load(value = "/testfile/phoneNumbers-international-rejected.csv") final String csv) throws IOException {
		var multipartFileMock = Mockito.mock(MultipartFile.class);
		when(multipartFileMock.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

		var result = CsvUtil.validateSmsCsv(multipartFileMock);

		assertThat(result.validEntries()).isEmpty();
		assertThat(result.invalidEntries()).containsExactlyInAnyOrder("+4712345678", "+12025551234", "+47701740610");
	}

	@Test
	void validateSmsCsvSwedishBoundaryLengths(@Load(value = "/testfile/phoneNumbers-swedish-boundary.csv") final String csv) throws IOException {
		var multipartFileMock = Mockito.mock(MultipartFile.class);
		when(multipartFileMock.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

		var result = CsvUtil.validateSmsCsv(multipartFileMock);

		assertThat(result.validEntries()).containsExactlyInAnyOrderEntriesOf(Map.of(
			"+46701740650", 1));
		assertThat(result.invalidEntries()).containsExactlyInAnyOrder("+4670174061", "+467017406100", "0046701740610");
	}

	@Test
	void validateSmsCsvIOException() throws IOException {
		var multipartFileMock = Mockito.mock(MultipartFile.class);

		var customExceptionMessage = "Test exception";
		when(multipartFileMock.getInputStream()).thenThrow(new IOException(customExceptionMessage));

		assertThatThrownBy(() -> CsvUtil.validateSmsCsv(multipartFileMock))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("Internal Server Error: Could not read CSV file: %s".formatted(customExceptionMessage));
	}

}
