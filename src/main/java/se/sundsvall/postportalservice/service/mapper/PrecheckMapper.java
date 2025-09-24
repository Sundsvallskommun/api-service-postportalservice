package se.sundsvall.postportalservice.service.mapper;

import static java.lang.Boolean.FALSE;
import static java.util.Optional.ofNullable;
import static se.sundsvall.postportalservice.service.PrecheckService.FAILURE_REASON_PARTY_ID_NOT_FOUND;
import static se.sundsvall.postportalservice.service.PrecheckService.FAILURE_REASON_UNKNOWN_ERROR;

import generated.se.sundsvall.citizen.CitizenExtended;
import generated.se.sundsvall.citizen.PersonGuidBatch;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;
import se.sundsvall.postportalservice.api.model.PrecheckResponse.DeliveryMethod;
import se.sundsvall.postportalservice.api.model.PrecheckResponse.PrecheckRecipient;

@Component
public final class PrecheckMapper {

	public Map<String, String> toFailureByPersonId(List<PersonGuidBatch> batches) {
		return batches.stream()
			.filter(personGuidBatch -> FALSE.equals(personGuidBatch.getSuccess()))
			.collect(LinkedHashMap::new,
				(map, personGuidBatch) -> map.put(
					personGuidBatch.getPersonNumber(),
					ofNullable(personGuidBatch.getErrorMessage())
						.map(String::trim)
						.filter(errorMessage -> !errorMessage.isEmpty())
						.orElse(FAILURE_REASON_UNKNOWN_ERROR)),
				Map::putAll);
	}

	public List<PrecheckRecipient> toRecipientsWithoutPartyIds(List<String> personIds, Map<String, String> failureByPersonId) {
		return personIds.stream()
			.map(personId -> {
				final var failure = failureByPersonId.get(personId);
				final var reason = failure != null
					? failure
					: FAILURE_REASON_PARTY_ID_NOT_FOUND;

				return new PrecheckRecipient(personId, null, DeliveryMethod.DELIVERY_NOT_POSSIBLE, reason);
			}).toList();
	}

	public List<String> toSnailMailEligiblePartyIds(List<CitizenExtended> citizens, Predicate<CitizenExtended> isEligible) {
		return citizens.stream()
			.filter(isEligible)
			.map(CitizenExtended::getPersonId)
			.filter(Objects::nonNull)
			.map(UUID::toString)
			.toList();
	}

	public Map<String, String> mapPersonIdToPartyId(List<PersonGuidBatch> personGuidBatches) {
		return personGuidBatches.stream()
			.collect(LinkedHashMap::new,
				(map, personGuidBatch) -> map.put(personGuidBatch.getPersonNumber(), personGuidBatch.getPersonId() == null
					? null
					: personGuidBatch.getPersonId().toString()),
				Map::putAll);
	}
}
