package q.qedet.finance.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import q.qedet.finance.entity.CashBox;
import java.util.*;

public interface CashBoxRepository extends JpaRepository<CashBox, UUID> {
    List<CashBox> findAllByOrganizationIdOrderByNameAsc(UUID organizationId);
    Optional<CashBox> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Optional<CashBox> findByOrganizationIdAndProvisioningKey(UUID organizationId, String provisioningKey);
    boolean existsByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CashBox c where c.id = :id and c.organization.id = :organizationId")
    Optional<CashBox> findLockedByIdAndOrganizationId(@Param("id") UUID id, @Param("organizationId") UUID organizationId);
}
