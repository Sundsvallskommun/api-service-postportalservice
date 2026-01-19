package se.sundsvall.postportalservice.service.util;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static se.sundsvall.postportalservice.integration.citizen.CitizenIntegration.POPULATION_REGISTRATION_ADDRESS;

import generated.se.sundsvall.citizen.CitizenExtended;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import se.sundsvall.postportalservice.service.util.PartyIdMappingHelper.PartyIdMapping;
import se.sundsvall.postportalservice.util.LegalIdUtil;

/**
 * Utility class for categorizing citizens by eligibility criteria (age, registration status).
 */
public final class CitizenCategorizationHelper {

	private CitizenCategorizationHelper() {}

	/**
	 * Predicate to filter adult citizens.
	 *
	 * @return predicate that returns true for adults over 18
	 */
	public static Predicate<SimplifiedCitizen> isAdult() {
		return citizen -> {
			if (citizen == null || citizen.legalId() == null) {
				return false;
			}
			return LegalIdUtil.isAnAdult(citizen.legalId());
		};
	}

	/**
	 * Creates a predicate to filter minor citizens.
	 *
	 * @return predicate that returns true for minors (< 18)
	 */
	public static Predicate<SimplifiedCitizen> isMinor() {
		return citizen -> {
			if (citizen == null || citizen.legalId() == null) {
				return false;
			}
			return !LegalIdUtil.isAnAdult(citizen.legalId());
		};
	}

	/**
	 * Converts CitizenExtended objects to SimplifiedCitizen records.
	 *
	 * @param  citizens         CitizenExtended objects to convert
	 * @param  partyIdToLegalId map from party ID to legal ID
	 * @return                  list of SimplifiedCitizen records
	 */
	public static List<SimplifiedCitizen> fromCitizenExtended(final List<CitizenExtended> citizens, final PartyIdMapping partyIdToLegalId) {

		return ofNullable(citizens).orElse(emptyList()).stream()
			.map(citizen -> {
				// Get the partyId so we can look up the legalId
				final var partyId = Optional.ofNullable(citizen.getPersonId())
					.map(UUID::toString)
					.orElse(null);
				final var legalId = partyIdToLegalId.partyIdToLegalId().get(partyId);
				final var isRegistered = isRegisteredInSweden(citizen);

				return new SimplifiedCitizen(partyId, legalId, isRegistered);
			})
			.toList();
	}

	private static boolean isRegisteredInSweden(final CitizenExtended citizen) {
		return ofNullable(citizen.getAddresses())
			.orElse(emptyList())
			.stream()
			.anyMatch(address -> POPULATION_REGISTRATION_ADDRESS.equals(address.getAddressType()));
	}

	/**
	 * Categorizes citizens into different eligibility groups for message delivery.
	 *
	 * @param  citizens list of citizens to categorize
	 * @return          categorized citizens grouped by eligibility
	 */
	public static CategorizedCitizens categorizeCitizens(final List<SimplifiedCitizen> citizens) {
		final var citizenList = ofNullable(citizens).orElse(emptyList());

		final var adultsInSweden = citizenList.stream()
			.filter(SimplifiedCitizen::isRegisteredInSweden)
			.filter(isAdult())
			.toList();

		final var minorsInSweden = citizenList.stream()
			.filter(SimplifiedCitizen::isRegisteredInSweden)
			.filter(isMinor())
			.toList();

		final var notInSweden = citizenList.stream()
			.filter(citizen -> !citizen.isRegisteredInSweden())
			.toList();

		return new CategorizedCitizens(adultsInSweden, minorsInSweden, notInSweden);
	}

	/**
	 * Lightweight citizen representation containing only fields needed for categorization.
	 *
	 * @param partyId              partyId
	 * @param legalId              legalId
	 * @param isRegisteredInSweden whether the citizen has a population registration address in Sweden
	 */
	public record SimplifiedCitizen(
		String partyId,
		String legalId,
		boolean isRegisteredInSweden) {
	}

	/**
	 * Record holding categorized citizens.
	 *
	 * @param eligibleAdults        adults registered in Sweden (eligible for snail mail)
	 * @param ineligibleMinors      minors registered in Sweden (ineligible due to being minors)
	 * @param notRegisteredInSweden citizens not registered in Sweden (undeliverable)
	 */
	public record CategorizedCitizens(
		List<SimplifiedCitizen> eligibleAdults,
		List<SimplifiedCitizen> ineligibleMinors,
		List<SimplifiedCitizen> notRegisteredInSweden) {
	}
}
