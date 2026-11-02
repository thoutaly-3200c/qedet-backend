package q.qedet.hr.onboarding.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.hr.onboarding.entity.OnboardingTask;
import java.util.*;
public interface OnboardingTaskRepository extends JpaRepository<OnboardingTask, UUID> {
    Optional<OnboardingTask> findByIdAndOrganizationId(UUID id, UUID organizationId);
    long countByOnboardingIdAndStatus(UUID onboardingId, q.qedet.hr.onboarding.model.OnboardingTaskStatus status);
}
