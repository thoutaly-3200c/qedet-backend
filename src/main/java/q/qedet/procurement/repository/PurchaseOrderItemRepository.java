package q.qedet.procurement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import q.qedet.procurement.entity.PurchaseOrderItem;

import java.util.List;
import java.util.UUID;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, UUID> {
    List<PurchaseOrderItem> findAllByPurchaseRequestItemId(UUID purchaseRequestItemId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from PurchaseOrderItem i where i.id = :id")
    java.util.Optional<PurchaseOrderItem> findLockedById(@Param("id") UUID id);
}
