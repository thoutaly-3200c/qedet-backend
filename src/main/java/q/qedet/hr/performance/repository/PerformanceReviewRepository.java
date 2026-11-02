package q.qedet.hr.performance.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.hr.performance.entity.PerformanceReview;
import java.util.*;
public interface PerformanceReviewRepository extends JpaRepository<PerformanceReview, UUID> {
    Optional<PerformanceReview> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
