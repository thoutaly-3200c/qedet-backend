package q.qedet.organization.policy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.organization.policy.entity.EmployeeWorkSchedule;
import q.qedet.organization.policy.model.AssignmentStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeWorkScheduleRepository extends JpaRepository<EmployeeWorkSchedule, UUID> {
    List<EmployeeWorkSchedule> findAllByOrganizationIdAndEmployeeId(UUID organizationId, UUID employeeId);

    Optional<EmployeeWorkSchedule> findFirstByOrganizationIdAndEmployeeIdAndStatusAndEffectiveFromLessThanEqualAndEffectiveToIsNullOrderByEffectiveFromDesc(
            UUID organizationId,
            UUID employeeId,
            AssignmentStatus status,
            LocalDate date
    );
}
