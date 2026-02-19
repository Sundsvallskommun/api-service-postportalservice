package se.sundsvall.postportalservice.api.validation.impl;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidCsvConstraintValidatorTest {

	@Mock
	private ConstraintValidatorContext context;

	@InjectMocks
	private ValidCsvConstraintValidator validCsvConstraintValidator;

	@Test
	void validMultipartFilesTest() {
		var file = Mockito.mock(MultipartFile.class);
		when(file.getContentType()).thenReturn("text/csv");

		var result = validCsvConstraintValidator.isValid(file, context);

		assertThat(result).isTrue();
	}

	@Test
	void invalidMultipartFilesTest() {
		var file = Mockito.mock(MultipartFile.class);
		when(file.getContentType()).thenReturn("application/pdf");

		var result = validCsvConstraintValidator.isValid(file, context);

		assertThat(result).isFalse();
	}

}
