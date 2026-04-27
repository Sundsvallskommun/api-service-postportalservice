package se.sundsvall.postportalservice.integration.party;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.postportalservice.integration.party.configuration.PartyProperties;

import static java.util.Collections.emptyMap;

@Component
public class PartyIntegration {

	private static final Logger LOG = LoggerFactory.getLogger(PartyIntegration.class);

	private final PartyClient partyClient;
	private final PartyProperties partyProperties;
	private final ExecutorService enterpriseLookupExecutor = Executors.newFixedThreadPool(16);

	public PartyIntegration(final PartyClient partyClient, final PartyProperties partyProperties) {
		this.partyClient = partyClient;
		this.partyProperties = partyProperties;
	}

	/**
	 * Get partyIds for the provided legalIds (PRIVATE, batch).
	 *
	 * @param  municipalityId the municipality id
	 * @param  legalIds       the legalIds
	 * @return                a map of legalId to partyId
	 */
	public Map<String, String> getPartyIds(final String municipalityId, final List<String> legalIds) {
		if (legalIds == null || legalIds.isEmpty()) {
			return emptyMap();
		}

		final var batchResult = new HashMap<String, String>();

		for (var i = 0; i < legalIds.size(); i += partyProperties.maxLegalIdsPerCall()) {
			final var partyIdsChunk = legalIds.subList(i, Math.min(i + partyProperties.maxLegalIdsPerCall(), legalIds.size()));
			batchResult.putAll(partyClient.getPartyIds(municipalityId, partyIdsChunk));
		}

		return batchResult;
	}

	/**
	 * Get legalIds for the provided partyIds (PRIVATE, batch).
	 *
	 * @param  municipalityId the municipality id
	 * @param  partyIds       the partyIds
	 * @return                a map of partyId to legalId
	 */
	public Map<String, String> getLegalIds(final String municipalityId, final List<String> partyIds) {
		if (partyIds == null || partyIds.isEmpty()) {
			return emptyMap();
		}

		final var batchResult = new HashMap<String, String>();

		for (var i = 0; i < partyIds.size(); i += partyProperties.maxPartyIdsPerCall()) {
			final var partyIdsChunk = partyIds.subList(i, Math.min(i + partyProperties.maxPartyIdsPerCall(), partyIds.size()));
			batchResult.putAll(partyClient.getPersonNumbers(municipalityId, partyIdsChunk));
		}

		return batchResult;
	}

	/**
	 * Get partyIds for the provided enterprise legalIds via parallel individual GET calls.
	 *
	 * @param  municipalityId the municipality id
	 * @param  legalIds       the enterprise legalIds (organization numbers)
	 * @return                a map of legalId to partyId; legalIds with no result are absent from the map
	 */
	public Map<String, String> getEnterprisePartyIds(final String municipalityId, final List<String> legalIds) {
		if (legalIds == null || legalIds.isEmpty()) {
			return emptyMap();
		}
		return fanOutLookup(legalIds, legalId -> partyClient.getEnterprisePartyIdByLegalId(municipalityId, legalId));
	}

	/**
	 * Get legalIds for the provided enterprise partyIds via parallel individual GET calls.
	 *
	 * @param  municipalityId the municipality id
	 * @param  partyIds       the enterprise partyIds
	 * @return                a map of partyId to legalId; partyIds with no result are absent from the map
	 */
	public Map<String, String> getEnterpriseLegalIds(final String municipalityId, final List<String> partyIds) {
		if (partyIds == null || partyIds.isEmpty()) {
			return emptyMap();
		}
		return fanOutLookup(partyIds, partyId -> partyClient.getEnterpriseLegalIdByPartyId(municipalityId, partyId));
	}

	private Map<String, String> fanOutLookup(final List<String> keys, final UnaryOperator<String> lookup) {
		final var futures = keys.stream()
			.map(key -> CompletableFuture
				.supplyAsync(() -> safeLookup(key, lookup), enterpriseLookupExecutor)
				.thenApply(value -> Map.entry(key, value == null ? "" : value)))
			.toList();

		final var result = new HashMap<String, String>();
		futures.forEach(future -> {
			final var entry = future.join();
			if (!entry.getValue().isEmpty()) {
				result.put(entry.getKey(), entry.getValue());
			}
		});
		return result;
	}

	private String safeLookup(final String key, final UnaryOperator<String> lookup) {
		try {
			return lookup.apply(key);
		} catch (final Exception e) {
			LOG.debug("Enterprise party lookup failed for key {}: {}", key, e.getMessage());
			return null;
		}
	}
}
