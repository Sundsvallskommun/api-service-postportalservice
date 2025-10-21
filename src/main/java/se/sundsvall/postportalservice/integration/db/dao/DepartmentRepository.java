package se.sundsvall.postportalservice.integration.db.dao;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.sundsvall.postportalservice.integration.db.DepartmentEntity;

@Repository
public interface DepartmentRepository extends JpaRepository<DepartmentEntity, String> {
	Optional<DepartmentEntity> findByOrganizationId(String organizationId);
}
