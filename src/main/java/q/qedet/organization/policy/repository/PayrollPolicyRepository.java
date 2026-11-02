package q.qedet.organization.policy.repository;
import org.springframework.data.jpa.repository.JpaRepository; import q.qedet.organization.policy.entity.PayrollPolicy; import java.time.LocalDate; import java.util.*;
public interface PayrollPolicyRepository extends JpaRepository<PayrollPolicy,UUID>{ Optional<PayrollPolicy> findFirstByOrganizationIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(UUID organizationId, LocalDate date); }
