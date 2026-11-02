package q.qedet.employee.repository;

import org.springframework.data.jpa.repository.JpaRepository;


import q.qedet.employee.entity.Employee;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    List<Employee> findAllByOrganizationId(UUID organizationId);
    Optional<Employee> findByIdAndOrganizationId(UUID id, UUID organizationId);
    List<Employee> findAllByUserId(UUID userId);
    Optional<Employee> findByUserIdAndOrganizationId(UUID userId, UUID organizationId);
    Optional<Employee> findByOrganizationIdAndEmployeeNumber(UUID organizationId, String employeeNumber);
    boolean existsByOrganizationIdAndEmployeeNumber(UUID organizationId, String employeeNumber);
    boolean existsBySiteId(UUID siteId);
    long countByOrganizationId(UUID organizationId);
}
