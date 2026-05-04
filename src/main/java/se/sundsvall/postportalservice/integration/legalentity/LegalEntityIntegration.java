package se.sundsvall.postportalservice.integration.legalentity;

import generated.se.sundsvall.legalentity.LegalEntity2;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;

@Component
public class LegalEntityIntegration {

	private static final Logger LOG = LoggerFactory.getLogger(LegalEntityIntegration.class);

	private final LegalEntityClient client;
	private final ExecutorService lookupExecutor = Executors.newFixedThreadPool(16);

	public LegalEntityIntegration(final LegalEntityClient client) {
		this.client = client;
	}

	@PreDestroy
	void shutdown() {
		lookupExecutor.shutdown();
	}

	/**
	 * Get legal entities for the provided partyIds via parallel individual GET calls. Per-id failures are soft (absent from
	 * the result map).
	 */
	public Map<String, LegalEntity2> getLegalEntities(final String municipalityId, final List<String> partyIds) {
		if (partyIds == null || partyIds.isEmpty()) {
			return emptyMap();
		}

		final var result = new ConcurrentHashMap<String, LegalEntity2>();

		// Spring Cloud OpenFeign's SpringDecoder lazily initializes its HttpMessageConverters list on first use;
		// concurrent first calls race, and the losers see an empty list ('messageConverters must not be empty').
		// Make the first call synchronously so the converter list is populated before fanning out the rest.
		final var firstPartyId = partyIds.getFirst();
		ofNullable(safeLookup(municipalityId, firstPartyId))
			.ifPresent(entity -> result.put(firstPartyId, entity));

		final var remaining = partyIds.subList(1, partyIds.size());
		if (!remaining.isEmpty()) {
			final var futures = remaining.stream()
				.map(partyId -> CompletableFuture
					.supplyAsync(() -> safeLookup(municipalityId, partyId), lookupExecutor)
					.thenAccept(legalEntity -> ofNullable(legalEntity)
						.ifPresent(entity -> result.put(partyId, entity))))
				.toArray(CompletableFuture[]::new);
			CompletableFuture.allOf(futures).join();
		}

		return result;
	}

	private LegalEntity2 safeLookup(final String municipalityId, final String partyId) {
		try {
			return client.getLegalEntity(municipalityId, partyId);
		} catch (final Exception e) {
			LOG.warn("LegalEntity lookup failed for partyId {}: {}", partyId, e.getMessage());
			return null;
		}
	}
}
