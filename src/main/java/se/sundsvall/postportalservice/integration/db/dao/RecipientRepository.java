package se.sundsvall.postportalservice.integration.db.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.sundsvall.postportalservice.integration.db.RecipientEntity;

@Repository
public interface RecipientRepository extends JpaRepository<RecipientEntity, String> {
}
