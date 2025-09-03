package se.sundsvall.postportalservice.api.validation.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
class NoDuplicateFileNamesConstraintValidatorTest {

	@Mock
	private ConstraintValidatorContext context;

	@InjectMocks
	private NoDuplicateFileNamesConstraintValidator noDuplicateFileNamesConstraintValidator;

	@Test
	void validMultipartFilesTest() {
		var multipartFile1 = Mockito.mock(MultipartFile.class);
		when(multipartFile1.getOriginalFilename()).thenReturn("file1.pdf");
		var multipartFile2 = Mockito.mock(MultipartFile.class);
		when(multipartFile2.getOriginalFilename()).thenReturn("file2.pdf");
		var files = List.of(multipartFile1, multipartFile2);

		var result = noDuplicateFileNamesConstraintValidator.isValid(files, context);

		assertThat(result).isTrue();
	}

	@Test
	void duplicatedNamesTest() {
		var multipartFile1 = Mockito.mock(MultipartFile.class);
		when(multipartFile1.getOriginalFilename()).thenReturn("duplicate-name.pdf");
		var multipartFile2 = Mockito.mock(MultipartFile.class);
		when(multipartFile2.getOriginalFilename()).thenReturn("duplicate-name.pdf");
		var files = List.of(multipartFile1, multipartFile2);

		var result = noDuplicateFileNamesConstraintValidator.isValid(files, context);

		assertThat(result).isFalse();
	}

	@Test
	void blankNameTest() {
		var multipartFile = Mockito.mock(MultipartFile.class);
		when(multipartFile.getOriginalFilename()).thenReturn("");
		var files = List.of(multipartFile);

		var constraintValidatorContextBuilder = Mockito.mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
		when(context.buildConstraintViolationWithTemplate(any())).thenReturn(constraintValidatorContextBuilder);
		when(constraintValidatorContextBuilder.addConstraintViolation()).thenReturn(context);

		var result = noDuplicateFileNamesConstraintValidator.isValid(files, context);

		assertThat(result).isFalse();
	}
}
