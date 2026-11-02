package q.qedet.organization.policy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.organization.policy.entity.DepartmentScheduleAssignment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentScheduleAssignmentRepository extends JpaRepository<DepartmentScheduleAssignment, UUID> {
    List<DepartmentScheduleAssignment> findAllByOrganizationId(UUID organizationId);

    List<DepartmentScheduleAssignment> findAllByOrganizationIdAndDepartmentId(UUID organizationId, UUID departmentId);
    Optional<DepartmentScheduleAssignment> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
