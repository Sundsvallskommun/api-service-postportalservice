package se.sundsvall.postportalservice.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.postportalservice.api.validation.ValidPdf;

/**
 * Single-file variant of {@link ValidPdfConstraintValidator}, so {@code @ValidPdf} can be placed on a single
 * {@link MultipartFile} part (e.g. the primary signing document) as well as on a list of files.
 */
public class ValidPdfFileConstraintValidator implements ConstraintValidator<ValidPdf, MultipartFile> {

	@Override
	public boolean isValid(final MultipartFile file, final ConstraintValidatorContext context) {
		// Null is left to the required-part handling to reject; this validator only checks the content type.
		return file == null || "application/pdf".equals(file.getContentType());
	}
}
