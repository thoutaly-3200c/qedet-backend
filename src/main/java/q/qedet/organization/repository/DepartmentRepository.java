package q.qedet.organization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.organization.entity.Department;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    List<Department> findAllByOrganizationId(UUID organizationId);
    Optional<Department> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Optional<Department> findByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);
    boolean existsByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);
}
