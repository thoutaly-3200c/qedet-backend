package q.qedet.finance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.finance.entity.CashTransfer;
import java.util.*;

public interface CashTransferRepository extends JpaRepository<CashTransfer, UUID> {
    Optional<CashTransfer> findByIdAndOrganizationId(UUID id, UUID organizationId);
    List<CashTransfer> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
