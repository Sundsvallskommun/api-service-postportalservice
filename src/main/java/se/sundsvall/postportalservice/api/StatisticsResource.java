package se.sundsvall.postportalservice.api;

import generated.se.sundsvall.messaging.ConstraintViolationProblem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.Problem;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.postportalservice.api.model.Statistics;
import se.sundsvall.postportalservice.service.StatisticsService;

import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;

@Validated
@RestController
@Tag(name = "Statistics Resources")
@RequestMapping("/{municipalityId}/statistics")
@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
	Problem.class, ConstraintViolationProblem.class
})))
@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
class StatisticsResource {

	private final StatisticsService statisticsService;

	StatisticsResource(final StatisticsService statisticsService) {
		this.statisticsService = statisticsService;
	}

	@Operation(summary = "Get general statistics for each department within the municipality", responses = {
		@ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
	})
	@GetMapping("/departments")
	ResponseEntity<List<Statistics>> getStatisticsByDepartment(
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "year", description = "Filter statistics by year") @RequestParam @Pattern(regexp = "[0-9]{4}", message = "must be a value between 0 and 9999") final String year,
		@Parameter(name = "month", description = "Filter statistics by month") @RequestParam @Pattern(regexp = "^0?[1-9]$|^1[0-2]$", message = "must be a value between 1 and 12") final String month) {

		final var statistics = statisticsService.getDepartmentStatistics(year, month);
		return ok(statistics);
	}

}
