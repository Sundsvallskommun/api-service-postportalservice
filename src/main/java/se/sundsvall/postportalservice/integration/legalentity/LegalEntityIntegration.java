package se.sundsvall.postportalservice.integration.legalentity;

import generated.se.sundsvall.legalentity.LegalEntity2;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

	/**
	 * Get a single legal entity by partyId. Soft-fails on errors / 404 (returns empty Optional).
	 */
	public Optional<LegalEntity2> getLegalEntity(final String municipalityId, final String partyId) {
		try {
			return ofNullable(client.getLegalEntity(municipalityId, partyId));
		} catch (final Exception e) {
			LOG.debug("LegalEntity lookup failed for partyId {}: {}", partyId, e.getMessage());
			return Optional.empty();
		}
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

		final var futures = partyIds.stream()
			.map(partyId -> CompletableFuture
				.supplyAsync(() -> safeLookup(municipalityId, partyId), lookupExecutor)
				.thenAccept(legalEntity -> ofNullable(legalEntity)
					.ifPresent(entity -> result.put(partyId, entity))))
			.toArray(CompletableFuture[]::new);

		CompletableFuture.allOf(futures).join();

		return result;
	}

	private LegalEntity2 safeLookup(final String municipalityId, final String partyId) {
		try {
			return client.getLegalEntity(municipalityId, partyId);
		} catch (final Exception e) {
			LOG.debug("LegalEntity lookup failed for partyId {}: {}", partyId, e.getMessage());
			return null;
		}
	}
}
