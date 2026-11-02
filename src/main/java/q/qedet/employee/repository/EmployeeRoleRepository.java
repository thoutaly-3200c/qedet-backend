package q.qedet.employee.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.employee.entity.EmployeeRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRoleRepository extends JpaRepository<EmployeeRole, UUID> {
    List<EmployeeRole> findAllByEmployeeIdAndEndedAtIsNull(UUID employeeId);
    List<EmployeeRole> findAllByEmployeeIdAndEndedAtIsNullAndRoleOrganizationId(UUID employeeId, UUID organizationId);
    Optional<EmployeeRole> findByIdAndEmployeeOrganizationId(UUID id, UUID organizationId);
    boolean existsByEmployeeIdAndRoleIdAndEndedAtIsNull(UUID employeeId, UUID roleId);
}
