package se.sundsvall.postportalservice.integration.db.dao;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;

@Repository
@CircuitBreaker(name = "recipientRepository")
public interface RecipientRepository extends JpaRepository<RecipientEntity, String> {
}
