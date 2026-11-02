package q.qedet.finance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.finance.entity.CashReconciliation;
import java.util.*;

public interface CashReconciliationRepository extends JpaRepository<CashReconciliation, UUID> {
    List<CashReconciliation> findAllByCashBoxIdAndOrganizationIdOrderByCreatedAtDesc(UUID cashBoxId, UUID organizationId);
}
