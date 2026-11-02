package q.qedet.organization.policy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.organization.policy.entity.WorkSchedule;
import q.qedet.organization.policy.model.WorkScheduleStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, UUID> {
    Optional<WorkSchedule> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<WorkSchedule> findAllByOrganizationId(UUID organizationId);

    Optional<WorkSchedule> findFirstByOrganizationIdAndOrganizationDefaultTrueAndStatus(
            UUID organizationId,
            WorkScheduleStatus status
    );
}
