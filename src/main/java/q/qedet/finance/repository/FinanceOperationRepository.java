package q.qedet.finance.repository;
import jakarta.persistence.LockModeType; import org.springframework.data.jpa.repository.JpaRepository; import org.springframework.data.jpa.repository.Lock; import org.springframework.data.jpa.repository.Query; import org.springframework.data.repository.query.Param; import q.qedet.finance.entity.FinanceOperation; import q.qedet.finance.model.FinanceWorkflowTypes.OperationStatus; import java.util.*;
public interface FinanceOperationRepository extends JpaRepository<FinanceOperation, UUID> {
 Optional<FinanceOperation> findByIdAndOrganizationId(UUID id, UUID organizationId); Optional<FinanceOperation> findByOrganizationIdAndIdempotencyKey(UUID organizationId, String idempotencyKey); List<FinanceOperation> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select o from FinanceOperation o where o.id = :id and o.organization.id = :organizationId") Optional<FinanceOperation> findLockedByIdAndOrganizationId(@Param("id") UUID id, @Param("organizationId") UUID organizationId);
 List<FinanceOperation> findAllByFinanceRequestIdAndOrganizationIdAndStatus(UUID requestId, UUID organizationId, OperationStatus status);
}
