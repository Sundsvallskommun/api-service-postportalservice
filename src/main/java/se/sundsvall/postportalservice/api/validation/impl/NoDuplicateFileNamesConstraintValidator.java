package se.sundsvall.postportalservice.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.postportalservice.api.validation.NoDuplicateFileNames;

public class NoDuplicateFileNamesConstraintValidator implements ConstraintValidator<NoDuplicateFileNames, List<MultipartFile>> {

	@Override
	public boolean isValid(final List<MultipartFile> value, final ConstraintValidatorContext context) {
		final var documentNames = value.stream()
			.map(MultipartFile::getOriginalFilename)
			.map(string -> Optional.ofNullable(string).map(String::toLowerCase).orElse(""))
			.toList();

		if (documentNames.contains("")) {
			context.disableDefaultConstraintViolation();
			context.buildConstraintViolationWithTemplate("empty filenames are not allowed")
				.addConstraintViolation();
			return false;
		}

		return new HashSet<>(documentNames).size() == documentNames.size();
	}
}
