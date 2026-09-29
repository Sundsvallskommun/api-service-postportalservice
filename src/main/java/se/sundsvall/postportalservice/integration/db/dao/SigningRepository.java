package se.sundsvall.postportalservice.integration.db.dao;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import java.util.List;
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

	/**
	 * Same as {@link #findByMessageId(String)} but only matches a case whose message belongs to the given municipality.
	 *
	 * @param  messageId      the id of the owning message
	 * @param  municipalityId the municipality the message must belong to
	 * @return                the signing case, or empty if the message has none in that municipality
	 */
	Optional<SigningEntity> findByMessageIdAndMessageMunicipalityId(String messageId, String municipalityId);

	List<SigningEntity> findAllByMessageIdIn(List<String> messageIds);

}
