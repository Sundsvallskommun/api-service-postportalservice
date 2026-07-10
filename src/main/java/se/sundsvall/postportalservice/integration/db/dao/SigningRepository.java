package se.sundsvall.postportalservice.integration.db.dao;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.sundsvall.postportalservice.integration.db.SigningEntity;

@Repository
@CircuitBreaker(name = "signingRepository")
public interface SigningRepository extends JpaRepository<SigningEntity, String> {

	/**
	 * Looks up the signing case for a given message. Used by the e-signing callback path to correlate an inbound event
	 * with its stored case, and replaces the previous {@code message.getSigning()} navigation (a nullable inverse
	 * {@code @OneToOne} that Hibernate cannot lazily proxy, causing an N+1 select per message row during list queries).
	 *
	 * @param  messageId the id of the owning message
	 * @return           the signing case, or empty if the message has none
	 */
	Optional<SigningEntity> findByMessageId(String messageId);
}
