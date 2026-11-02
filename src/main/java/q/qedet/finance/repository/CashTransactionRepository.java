package q.qedet.finance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.finance.entity.CashTransaction;
import java.util.*;

public interface CashTransactionRepository extends JpaRepository<CashTransaction, UUID> {
    Optional<CashTransaction> findByIdAndOrganizationId(UUID id, UUID organizationId);
    List<CashTransaction> findAllByOrganizationIdOrderByPostedAtDesc(UUID organizationId);
    List<CashTransaction> findAllByCashBoxIdAndOrganizationIdOrderByPostedAtDesc(UUID cashBoxId, UUID organizationId);
}
