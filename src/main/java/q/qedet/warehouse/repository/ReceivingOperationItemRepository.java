package q.qedet.warehouse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import q.qedet.warehouse.entity.ReceivingOperationItem;

import java.math.BigDecimal;
import java.util.*;

public interface ReceivingOperationItemRepository extends JpaRepository<ReceivingOperationItem, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from ReceivingOperationItem i where i.id = :id")
    Optional<ReceivingOperationItem> findForUpdate(@Param("id") UUID id);
    @Query("select i from ReceivingOperationItem i join i.receivingRequestItem r join r.purchaseOrderItem p where p.purchaseRequestItem.product.id = :productId and (i.product is null or i.product.id = :productId) and p.purchaseOrder.organization.id = :organizationId and p.supplier.organization.id = :organizationId and p.supplier.status = q.qedet.procurement.entity.Supplier.SupplierStatus.Active and i.receivingOperation.organization.id = :organizationId and r.receivingRequest.organization.id = :organizationId and r.receivingRequest.purchaseOrder.id = p.purchaseOrder.id and i.receivingOperation.receivingRequest.id = r.receivingRequest.id and i.receivingOperation.status = q.qedet.warehouse.entity.ReceivingOperation.Status.COMPLETED and i.acceptedQuantity > 0")
    List<ReceivingOperationItem> eligibleHistory(@Param("organizationId") UUID organizationId,@Param("productId") UUID productId);
    List<ReceivingOperationItem> findAllByReceivingRequestItemIdOrderByReceivingOperationStartedAtAsc(UUID id);
    Optional<ReceivingOperationItem> findByReceivingOperationIdAndReceivingRequestItemId(UUID operationId, UUID requestItemId);

    /** Only the accumulated quantity is needed during record(); never hydrate operation-item graphs for it. */
    @Query("""
            select coalesce(sum(item.receivedQuantity), 0)
            from ReceivingOperationItem item
            where item.receivingRequestItem.id = :receivingRequestItemId
              and item.receivingOperation.id <> :receivingOperationId
              and item.receivingOperation.status <> q.qedet.warehouse.entity.ReceivingOperation.Status.CANCELLED
            """)
    BigDecimal receivedQuantityForOtherOperations(
            @Param("receivingRequestItemId") UUID receivingRequestItemId,
            @Param("receivingOperationId") UUID receivingOperationId);

    /** Cancelled operations are discarded work and therefore do not count as received. */
    @Query("""
            select coalesce(sum(item.receivedQuantity), 0)
            from ReceivingOperationItem item
            where item.receivingRequestItem.id = :receivingRequestItemId
              and item.receivingOperation.status <> q.qedet.warehouse.entity.ReceivingOperation.Status.CANCELLED
            """)
    BigDecimal receivedQuantityForRequestItem(
            @Param("receivingRequestItemId") UUID receivingRequestItemId);
}
