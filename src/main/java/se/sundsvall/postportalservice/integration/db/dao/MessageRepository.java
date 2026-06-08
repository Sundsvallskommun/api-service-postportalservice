package se.sundsvall.postportalservice.integration.db.dao;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.sundsvall.postportalservice.integration.db.MessageEntity;
import se.sundsvall.postportalservice.integration.db.converter.MessageType;

@Repository
@CircuitBreaker(name = "messageRepository")
public interface MessageRepository extends JpaRepository<MessageEntity, String> {
	Page<MessageEntity> findAllByMunicipalityIdAndUserUsernameIgnoreCase(final String municipalityId, final String username, final Pageable pageable);

	Optional<MessageEntity> findByMunicipalityIdAndIdAndUserUsernameIgnoreCase(final String municipalityId, final String messageId, final String username);

	Optional<MessageEntity> findByIdAndMessageType(final String messageId, final MessageType messageType);
}
