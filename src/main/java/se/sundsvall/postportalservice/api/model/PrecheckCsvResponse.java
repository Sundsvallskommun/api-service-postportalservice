package se.sundsvall.postportalservice.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "Model used as response when validating csv format and duplicate entries")
public record PrecheckCsvResponse(Map<String, Integer> duplicateEntries) {
}
