package q.qedet.hr.performance.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.hr.performance.entity.Goal;
import java.util.*;
public interface GoalRepository extends JpaRepository<Goal, UUID> {
    Optional<Goal> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
