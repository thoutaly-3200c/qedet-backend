package q.qedet.hr.recruitment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.hr.recruitment.entity.Interview;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterviewRepository extends JpaRepository<Interview, UUID> {
    List<Interview> findAllByApplicationIdAndOrganizationIdOrderByScheduledAtAsc(UUID applicationId, UUID organizationId);
    Optional<Interview> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
