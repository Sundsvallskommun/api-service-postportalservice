package se.sundsvall.postportalservice.integration.db.dao;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.sundsvall.postportalservice.integration.db.DepartmentEntity;

@Repository
@CircuitBreaker(name = "departmentRepository")
public interface DepartmentRepository extends JpaRepository<DepartmentEntity, String> {
	Optional<DepartmentEntity> findByOrganizationId(String organizationId);
}
