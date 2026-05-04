package se.sundsvall.postportalservice.integration.party;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.postportalservice.integration.db.converter.PartyType;
import se.sundsvall.postportalservice.integration.party.configuration.PartyProperties;

import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;

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

	/**
	 * Resolve a {@link PartyType} for each provided partyId. Combines the PRIVATE batch lookup with an ENTERPRISE per-id
	 * fan-out for partyIds the PRIVATE batch did not resolve. PartyIds that match neither are absent from the result map;
	 * callers decide how
	 * to treat unknowns.
	 *
	 * @param  municipalityId the municipality id
	 * @param  partyIds       the partyIds to classify
	 * @return                a map of partyId to {@link PartyType}; unresolved partyIds are absent
	 */
	public Map<String, PartyType> getPartyTypes(final String municipalityId, final List<String> partyIds) {
		if (partyIds == null || partyIds.isEmpty()) {
			return emptyMap();
		}

		final var result = new HashMap<String, PartyType>();
		getLegalIds(municipalityId, partyIds).keySet()
			.forEach(partyId -> result.put(partyId, PartyType.PRIVATE));

		final var unresolved = partyIds.stream()
			.filter(partyId -> !result.containsKey(partyId))
			.toList();
		getEnterpriseLegalIds(municipalityId, unresolved).keySet()
			.forEach(partyId -> result.put(partyId, PartyType.ENTERPRISE));

		return result;
	}

	private Map<String, String> fanOutLookup(final List<String> keys, final UnaryOperator<String> lookup) {
		final var result = new ConcurrentHashMap<String, String>();
		if (keys.isEmpty()) {
			return result;
		}

		// Spring Cloud OpenFeign's SpringDecoder lazily initializes its HttpMessageConverters list on first use;
		// concurrent first calls race, and the losers see an empty list ('messageConverters must not be empty').
		// Make the first call synchronously so the converter list is populated before fanning out the rest.
		final var firstKey = keys.getFirst();
		ofNullable(safeLookup(firstKey, lookup))
			.filter(v -> !v.isEmpty())
			.ifPresent(v -> result.put(firstKey, v));

		final var remaining = keys.subList(1, keys.size());
		if (!remaining.isEmpty()) {
			final var futures = remaining.stream()
				.map(key -> CompletableFuture
					.supplyAsync(() -> safeLookup(key, lookup), enterpriseLookupExecutor)
					.thenAccept(value -> ofNullable(value)
						.filter(v -> !v.isEmpty())
						.ifPresent(v -> result.put(key, v))))
				.toArray(CompletableFuture[]::new);
			CompletableFuture.allOf(futures).join();
		}

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
