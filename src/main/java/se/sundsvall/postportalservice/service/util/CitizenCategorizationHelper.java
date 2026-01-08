package se.sundsvall.postportalservice.service.util;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

import generated.se.sundsvall.citizen.CitizenExtended;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import se.sundsvall.postportalservice.util.LegalIdUtil;

/**
 * Utility class for categorizing citizens by eligibility criteria (age, registration status).
 */
public final class CitizenCategorizationHelper {

	private CitizenCategorizationHelper() {}

	/**
	 * Predicate to filter adult citizens based on partyId-to-legalId mapping.
	 *
	 * @param  partyIdToLegalId map from party ID to legalId
	 * @return                  predicate that returns true for adults over 18
	 */
	public static Predicate<CitizenExtended> isAdult(final Map<String, String> partyIdToLegalId) {
		return citizen -> {
			final var partyId = extractPartyId(citizen);
			if (partyId == null) {
				return false;
			}
			final var legalId = partyIdToLegalId.get(partyId);
			return LegalIdUtil.isAnAdult(legalId);
		};
	}

	/**
	 * Creates a predicate to filter minor citizens based on partyId-to-legalId mapping.
	 *
	 * @param  partyIdToLegalId map from party ID to legalId
	 * @return                  predicate that returns true for minors (< 18)
	 */
	public static Predicate<CitizenExtended> isMinor(final Map<String, String> partyIdToLegalId) {
		return citizen -> {
			final var partyId = extractPartyId(citizen);
			if (partyId == null) {
				return false;
			}
			final var legalId = partyIdToLegalId.get(partyId);
			return legalId != null && !LegalIdUtil.isAnAdult(legalId);
		};
	}

	/**
	 * Categorizes citizens into different eligibility groups for message delivery.
	 *
	 * @param  citizens             list of citizens to categorize
	 * @param  partyIdTolegalId     map from party ID to legalId for age verification
	 * @param  isRegisteredInSweden predicate to check if citizen is registered in Sweden
	 * @return                      categorized citizens grouped by eligibility
	 */
	public static CategorizedCitizens categorizeCitizens(final List<CitizenExtended> citizens, final Map<String, String> partyIdTolegalId,
		final Predicate<CitizenExtended> isRegisteredInSweden) {

		final var citizenList = ofNullable(citizens).orElse(emptyList());

		final var adultsInSweden = citizenList.stream()
			.filter(isRegisteredInSweden)
			.filter(isAdult(partyIdTolegalId))
			.toList();

		final var minorsInSweden = citizenList.stream()
			.filter(isRegisteredInSweden)
			.filter(isMinor(partyIdTolegalId))
			.toList();

		final var notInSweden = citizenList.stream()
			.filter(isRegisteredInSweden.negate())
			.toList();

		return new CategorizedCitizens(adultsInSweden, minorsInSweden, notInSweden);
	}

	/**
	 * Extracts partyId from a CitizenExtended object.
	 *
	 * @param  citizen the citizen object
	 * @return         partyId as string, null if not present
	 */
	private static String extractPartyId(final CitizenExtended citizen) {
		return Optional.ofNullable(citizen.getPersonId())
			.map(UUID::toString)
			.orElse(null);
	}

	/**
	 * Record holding categorized citizens.
	 *
	 * @param eligibleAdults        adults registered in Sweden (eligible for snail mail)
	 * @param ineligibleMinors      minors registered in Sweden (ineligible due to being minors)
	 * @param notRegisteredInSweden citizens not registered in Sweden (undeliverable)
	 */
	public record CategorizedCitizens(
		List<CitizenExtended> eligibleAdults,
		List<CitizenExtended> ineligibleMinors,
		List<CitizenExtended> notRegisteredInSweden) {
	}
}
