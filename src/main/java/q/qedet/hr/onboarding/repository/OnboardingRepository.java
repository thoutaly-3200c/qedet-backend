package q.qedet.hr.onboarding.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.hr.onboarding.entity.Onboarding;
import java.util.*;
public interface OnboardingRepository extends JpaRepository<Onboarding, UUID> {
    Optional<Onboarding> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Optional<Onboarding> findByEmployeeIdAndOrganizationId(UUID employeeId, UUID organizationId);
}
