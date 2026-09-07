package se.sundsvall.postportalservice.integration.objectstore;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import se.sundsvall.postportalservice.integration.objectstore.configuration.ObjectStoreConfiguration;

import static org.springframework.http.MediaType.ALL_VALUE;
import static se.sundsvall.postportalservice.integration.objectstore.configuration.ObjectStoreConfiguration.CLIENT_ID;

@FeignClient(
	name = CLIENT_ID,
	url = "${integration.object-store.url}",
	configuration = ObjectStoreConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface ObjectStoreClient {

	/**
	 * Stores an object under an id the caller chooses. The store replaces rather than rejects on a repeat, so a retried
	 * upload of the same id is harmless.
	 *
	 * @param expiresAt when the object may be removed; the store applies its own default if left out
	 */
	@PutMapping(path = "/objects/{bucket}/{objectId}", consumes = ALL_VALUE)
	ResponseEntity<Void> storeObject(
		@PathVariable final String bucket,
		@PathVariable final String objectId,
		@RequestParam(name = "expiresAt", required = false) final String expiresAt,
		@RequestHeader("Content-Type") final String contentType,
		@RequestHeader("Content-Disposition") final String contentDisposition,
		@RequestBody final byte[] content);
}
