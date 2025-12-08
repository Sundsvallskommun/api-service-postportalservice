package se.sundsvall.postportalservice.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

@Schema(description = "Precheck request model")
public record PrecheckRequest(
	@Schema(description = "List of party ids to precheck", examples = "[\"b46f0ca2-d2ad-43e8-8d50-3aeb949e3604\", \"fd99a03c-790c-4b87-bc4b-f4f73e4a2df4\"]") @NotEmpty List<@ValidUuid String> partyIds) {
}
