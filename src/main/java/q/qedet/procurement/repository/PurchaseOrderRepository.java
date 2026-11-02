
 package q.qedet.procurement.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import q.qedet.procurement.entity.PurchaseOrder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {

    List<PurchaseOrder> findAllByOrganizationId(UUID organizationId);

    Optional<PurchaseOrder> findByIdAndOrganizationId(
            UUID id, UUID organizationId);

    @Query("""
        SELECT DISTINCT o
        FROM PurchaseOrder o
        LEFT JOIN FETCH o.items
        WHERE o.id = :id
          AND o.organization.id = :organizationId
        """)
    Optional<PurchaseOrder> findByIdWithItemsAndOrganizationId(
            @Param("id") UUID id,
            @Param("organizationId") UUID organizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT o
        FROM PurchaseOrder o
        WHERE o.id = :id
          AND o.organization.id = :organizationId
        """)
    Optional<PurchaseOrder> findLockedByIdAndOrganizationId(
            @Param("id") UUID id,
            @Param("organizationId") UUID organizationId);
}
