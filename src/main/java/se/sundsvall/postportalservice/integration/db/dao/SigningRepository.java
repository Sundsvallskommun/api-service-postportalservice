package se.sundsvall.postportalservice.integration.db.dao;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.sundsvall.postportalservice.integration.db.SigningEntity;

@Repository
@CircuitBreaker(name = "signingRepository")
public interface SigningRepository extends JpaRepository<SigningEntity, String> {

	Optional<SigningEntity> findByProviderCaseId(final String providerCaseId);
}
