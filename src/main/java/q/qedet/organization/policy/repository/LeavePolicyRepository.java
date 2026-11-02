package q.qedet.organization.policy.repository;
import org.springframework.data.jpa.repository.JpaRepository; import q.qedet.organization.policy.entity.LeavePolicy; import java.time.LocalDate; import java.util.*;
public interface LeavePolicyRepository extends JpaRepository<LeavePolicy,UUID>{ Optional<LeavePolicy> findFirstByOrganizationIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(UUID organizationId, LocalDate date); }
