package q.qedet.hr.recruitment.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.hr.recruitment.entity.Application;
import java.util.*;
public interface ApplicationRepository extends JpaRepository<Application, UUID> {
    Optional<Application> findByIdAndOrganizationId(UUID id, UUID organizationId);
    boolean existsByCandidateIdAndJobOpeningId(UUID candidateId, UUID jobOpeningId);
    boolean existsByCandidateIdAndOrganizationId(UUID candidateId, UUID organizationId);
    List<Application> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    List<Application> findAllByJobOpeningIdAndOrganizationIdOrderByCreatedAtDesc(UUID jobOpeningId, UUID organizationId);
    List<Application> findAllByCandidateIdOrderByCreatedAtDesc(UUID candidateId);
}
