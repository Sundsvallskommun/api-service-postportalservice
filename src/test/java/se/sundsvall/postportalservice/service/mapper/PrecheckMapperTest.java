package se.sundsvall.postportalservice.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.postportalservice.service.PrecheckService.FAILURE_REASON_PARTY_ID_NOT_FOUND;
import static se.sundsvall.postportalservice.service.PrecheckService.FAILURE_REASON_UNKNOWN_ERROR;

import generated.se.sundsvall.citizen.CitizenExtended;
import generated.se.sundsvall.citizen.PersonGuidBatch;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import se.sundsvall.postportalservice.api.model.PrecheckResponse.DeliveryMethod;
import se.sundsvall.postportalservice.api.model.PrecheckResponse.PrecheckRecipient;

class PrecheckMapperTest {

	private final PrecheckMapper precheckMapper = new PrecheckMapper();

	@Test
	void toFailureByPersonId() {
		final var personGuidBatchOK = createPersonGuidBatch("191111111111", true, null, UUID.randomUUID());
		final var personGuidBatchFail1 = createPersonGuidBatch("192222222222", false, "some random error", null);
		final var personGuidBatchFail2 = createPersonGuidBatch("193333333333", false, null, null);
		final var personGuidBatches = List.of(personGuidBatchOK, personGuidBatchFail1, personGuidBatchFail2);

		final var result = precheckMapper.toFailureByPersonId(personGuidBatches);

		assertThat(result).containsOnlyKeys("192222222222", "193333333333");
		assertThat(result.get("192222222222")).isEqualTo("some random error");
		assertThat(result.get("193333333333")).isEqualTo(FAILURE_REASON_UNKNOWN_ERROR);
	}

	@Test
	void mapPersonIdToPartyId() {
		final var personGuidBatch1 = createPersonGuidBatch("191111111111", true, null, UUID.fromString("11111111-1111-1111-1111-111111111111"));
		final var personGuidBatch2 = createPersonGuidBatch("192222222222", true, null, null);

		final var result = precheckMapper.mapPersonIdToPartyId(List.of(personGuidBatch1, personGuidBatch2));

		assertThat(result)
			.containsEntry("191111111111", "11111111-1111-1111-1111-111111111111")
			.containsEntry("192222222222", null);
	}

	@Test
	void toRecipientsWithoutPartyIds() {
		final var personIds = List.of("191111111111", "192222222222");
		final var failures = Map.of("192222222222", "some random error");

		final var results = precheckMapper.toRecipientsWithoutPartyIds(personIds, failures);

		assertThat(results).hasSize(2);
		assertThat(results.get(0))
			.extracting(
				PrecheckRecipient::personalIdentityNumber,
				PrecheckRecipient::deliveryMethod,
				PrecheckRecipient::partyId,
				PrecheckRecipient::reason)
			.containsExactly("191111111111", DeliveryMethod.DELIVERY_NOT_POSSIBLE, null, FAILURE_REASON_PARTY_ID_NOT_FOUND);
		assertThat(results.get(1).reason()).isEqualTo("some random error");
	}

	@Test
	void toSnailMailEligiblePartyIds() {
		final var citizen1 = createCitizen("11111111-1111-1111-1111-111111111111");
		final var citizen2 = createCitizen(null);
		final var citizen3 = createCitizen("22222222-2222-2222-2222-222222222222");
		final var citizens = List.of(citizen1, citizen2, citizen3);

		Predicate<CitizenExtended> isEligible = citizen -> citizen.getPersonId() != null && citizen.getPersonId().toString().startsWith("1");

		final var results = precheckMapper.toSnailMailEligiblePartyIds(citizens, isEligible);

		assertThat(results).containsExactly("11111111-1111-1111-1111-111111111111");
	}

	private PersonGuidBatch createPersonGuidBatch(String personNumber, boolean success, String errorMessage, UUID personId) {
		final var personGuidBatch = new PersonGuidBatch();

		personGuidBatch.setPersonNumber(personNumber);
		personGuidBatch.setSuccess(success);
		personGuidBatch.setErrorMessage(errorMessage);
		personGuidBatch.setPersonId(personId);

		return personGuidBatch;
	}

	private CitizenExtended createCitizen(String personId) {
		final var citizen = new CitizenExtended();

		if (personId != null) {
			citizen.setPersonId(UUID.fromString(personId));
		}

		return citizen;
	}
}
