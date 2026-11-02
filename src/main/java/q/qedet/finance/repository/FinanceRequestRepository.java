package q.qedet.finance.repository;
import jakarta.persistence.LockModeType; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import q.qedet.finance.entity.FinanceRequest; import java.util.*;
public interface FinanceRequestRepository extends JpaRepository<FinanceRequest, UUID> {
 Optional<FinanceRequest> findByIdAndOrganizationId(UUID id, UUID organizationId); List<FinanceRequest> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from FinanceRequest r where r.id = :id and r.organization.id = :organizationId") Optional<FinanceRequest> findLockedByIdAndOrganizationId(@Param("id") UUID id, @Param("organizationId") UUID organizationId);
}
