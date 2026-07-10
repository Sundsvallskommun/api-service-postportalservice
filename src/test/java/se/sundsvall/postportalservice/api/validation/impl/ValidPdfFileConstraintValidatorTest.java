package se.sundsvall.postportalservice.api.validation.impl;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidPdfFileConstraintValidatorTest {

	@Mock
	private ConstraintValidatorContext context;

	@InjectMocks
	private ValidPdfFileConstraintValidator validPdfFileConstraintValidator;

	@Test
	void validPdfFileTest() {
		var multipartFile = mock(MultipartFile.class);
		when(multipartFile.getContentType()).thenReturn("application/pdf");

		assertThat(validPdfFileConstraintValidator.isValid(multipartFile, context)).isTrue();
	}

	@Test
	void invalidPdfFileTest() {
		var multipartFile = mock(MultipartFile.class);
		when(multipartFile.getContentType()).thenReturn("text/plain");

		assertThat(validPdfFileConstraintValidator.isValid(multipartFile, context)).isFalse();
	}

	@Test
	void nullFileTest() {
		assertThat(validPdfFileConstraintValidator.isValid(null, context)).isTrue();
	}
}
