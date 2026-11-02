package q.qedet.finance.repository;
import org.springframework.data.jpa.repository.JpaRepository; import q.qedet.finance.entity.FinanceLedgerEntry; import java.util.*;
public interface FinanceLedgerEntryRepository extends JpaRepository<FinanceLedgerEntry, UUID> { List<FinanceLedgerEntry> findAllByAccountIdAndOrganizationIdOrderByPostedAtDesc(UUID accountId, UUID organizationId); List<FinanceLedgerEntry> findAllByOperationIdOrderByCreatedAtAsc(UUID operationId); }
