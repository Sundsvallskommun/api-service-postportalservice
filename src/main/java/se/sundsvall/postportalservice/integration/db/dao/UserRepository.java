package se.sundsvall.postportalservice.integration.db.dao;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.sundsvall.postportalservice.integration.db.UserEntity;

@Repository
@CircuitBreaker(name = "userRepository")
public interface UserRepository extends JpaRepository<UserEntity, String> {
	Optional<UserEntity> findByUsernameIgnoreCase(String username);
}
