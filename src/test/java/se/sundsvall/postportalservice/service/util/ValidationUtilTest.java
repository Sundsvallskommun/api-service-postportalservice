package se.sundsvall.postportalservice.service.util;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import se.sundsvall.postportalservice.api.model.LetterRequest;

class ValidationUtilTest {

	@Test
	void ensureFailureThrows() {
		// Empty request should fail validation
		var letterRequest = new LetterRequest();

		assertThatThrownBy(() -> ValidationUtil.validate(letterRequest))
			.isInstanceOf(ConstraintViolationException.class);
	}
}
