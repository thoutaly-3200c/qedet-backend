package q.qedet.organization.policy.repository;
import org.springframework.data.jpa.repository.JpaRepository; import q.qedet.organization.policy.entity.AttendancePolicy; import java.time.LocalDate; import java.util.*;
public interface AttendancePolicyRepository extends JpaRepository<AttendancePolicy,UUID>{ Optional<AttendancePolicy> findFirstByOrganizationIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(UUID organizationId, LocalDate date); }
