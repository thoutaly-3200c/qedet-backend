package q.qedet.finance.repository;
import org.springframework.data.jpa.repository.JpaRepository; import q.qedet.finance.entity.FinanceTransferConfirmation; import java.util.*;
public interface FinanceTransferConfirmationRepository extends JpaRepository<FinanceTransferConfirmation, UUID> { Optional<FinanceTransferConfirmation> findByOperationIdAndOrganizationId(UUID operationId, UUID organizationId); Optional<FinanceTransferConfirmation> findByQrTokenAndOrganizationId(String qrToken, UUID organizationId); }
