package q.qedet.hr.compensation.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.hr.compensation.entity.CompensationAllowance;
import java.util.*;
public interface CompensationAllowanceRepository extends JpaRepository<CompensationAllowance, UUID> {
    Optional<CompensationAllowance> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
