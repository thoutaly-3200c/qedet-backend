package q.qedet.hr.compensation.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.hr.compensation.entity.CompensationHistory;
import java.util.*;
public interface CompensationHistoryRepository extends JpaRepository<CompensationHistory, UUID> {
    List<CompensationHistory> findAllByEmployeeIdAndOrganizationIdOrderByEffectiveFromDesc(UUID employeeId, UUID organizationId);
}
