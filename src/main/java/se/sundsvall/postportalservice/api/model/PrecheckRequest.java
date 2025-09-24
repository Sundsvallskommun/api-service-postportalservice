package se.sundsvall.postportalservice.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "Precheck request model")
public record PrecheckRequest(
	@Schema(description = "List of personal identity numbers to precheck", example = "[\"191111111111\", \"192222222222\"]") @NotEmpty List<String> personIds) {}
