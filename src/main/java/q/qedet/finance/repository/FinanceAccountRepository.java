package q.qedet.finance.repository;
import jakarta.persistence.LockModeType; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import q.qedet.finance.entity.FinanceAccount; import java.util.*;
public interface FinanceAccountRepository extends JpaRepository<FinanceAccount, UUID> {
 Optional<FinanceAccount> findByIdAndOrganizationId(UUID id, UUID organizationId); List<FinanceAccount> findAllByOrganizationIdOrderByNameAsc(UUID organizationId);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select a from FinanceAccount a where a.id = :id and a.organization.id = :organizationId") Optional<FinanceAccount> findLockedByIdAndOrganizationId(@Param("id") UUID id, @Param("organizationId") UUID organizationId);
}
