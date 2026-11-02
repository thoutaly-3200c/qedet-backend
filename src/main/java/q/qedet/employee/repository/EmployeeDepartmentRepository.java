package q.qedet.employee.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.employee.entity.EmployeeDepartment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeDepartmentRepository extends JpaRepository<EmployeeDepartment, UUID> {
    List<EmployeeDepartment> findAllByEmployeeIdAndEndedAtIsNull(UUID employeeId);
    Optional<EmployeeDepartment> findByIdAndEmployeeOrganizationId(UUID id, UUID organizationId);
    boolean existsByEmployeeIdAndDepartmentIdAndEndedAtIsNull(UUID employeeId, UUID departmentId);
    boolean existsByEmployeeIdAndPrimaryAssignmentIsTrueAndEndedAtIsNull(UUID employeeId);
}
