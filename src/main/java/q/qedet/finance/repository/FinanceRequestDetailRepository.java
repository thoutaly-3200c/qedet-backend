package q.qedet.finance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.finance.entity.FinanceRequestDetail;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FinanceRequestDetailRepository extends JpaRepository<FinanceRequestDetail, UUID> {
    Optional<FinanceRequestDetail> findByRequestId(UUID requestId);
    List<FinanceRequestDetail> findAllByOriginalOperationId(UUID originalOperationId);
}
