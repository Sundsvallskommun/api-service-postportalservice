package se.sundsvall.postportalservice.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.postportalservice.api.validation.ValidPdf;

public class ValidPdfConstraintValidator implements ConstraintValidator<ValidPdf, List<MultipartFile>> {

	@Override
	public boolean isValid(final List<MultipartFile> files, final ConstraintValidatorContext context) {
		// Null is left to @NotEmpty/@NotNull to reject; this validator only checks the content type of present files.
		return files == null || files.stream()
			.map(MultipartFile::getContentType)
			.allMatch("application/pdf"::equals);
	}
}
