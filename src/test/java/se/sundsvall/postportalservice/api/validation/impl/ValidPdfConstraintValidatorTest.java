package se.sundsvall.postportalservice.api.validation.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class ValidPdfConstraintValidatorTest {

	@Mock
	private ConstraintValidatorContext context;

	@InjectMocks
	private ValidPdfConstraintValidator validPdfConstraintValidator;

	@Test
	void validMultipartFilesTest() {
		var multipartFile = Mockito.mock(MultipartFile.class);
		when(multipartFile.getContentType()).thenReturn("application/pdf");
		var files = List.of(multipartFile, multipartFile);

		var result = validPdfConstraintValidator.isValid(files, context);

		assertThat(result).isTrue();
	}

	@Test
	void invalidMultipartFilesTest() {
		var multipartFile = Mockito.mock(MultipartFile.class);
		when(multipartFile.getContentType()).thenReturn("text/plain");
		var files = List.of(multipartFile, multipartFile);

		var result = validPdfConstraintValidator.isValid(files, context);

		assertThat(result).isFalse();
	}
}
