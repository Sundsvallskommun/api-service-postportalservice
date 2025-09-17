package se.sundsvall.postportalservice.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Result of precheck for uploaded recipients")
public record PrecheckResponse(
	@Schema(description = "Per-recipient result") List<RecipientPrecheck> recipients) {

	@Schema(description = "Per-recipient delivery capability")
	public record RecipientPrecheck(
		@Schema(description = "Personal identity number of the recipient", example = "19111111-1111") String personalIdentityNumber,
		@Schema(description = "Party ID of the recipient", example = "da03b33e-9de2-45ac-8291-31a88de59410") String partyId,
		@Schema(description = "Delivery method for the recipient") DeliveryMethod deliveryMethod,
		@Schema(description = "Reason when delivery method isn't available or an upstream lookup failed", example = "Person not found") String reason) {}

	@Schema(enumAsRef = true, description = "Possible delivery methods")
	public enum DeliveryMethod {
		DIGITAL_MAIL,
		SNAIL_MAIL,
		DELIVERY_NOT_POSSIBLE
	}

	public static PrecheckResponse of(List<RecipientPrecheck> recipients) {
		return new PrecheckResponse(recipients);
	}
}
