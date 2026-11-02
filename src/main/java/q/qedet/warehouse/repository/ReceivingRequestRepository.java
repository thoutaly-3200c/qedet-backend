package q.qedet.warehouse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import q.qedet.warehouse.entity.ReceivingRequest;

import java.math.BigDecimal;
import java.util.*;

public interface ReceivingRequestRepository extends JpaRepository<ReceivingRequest, UUID> {
    List<ReceivingRequest> findAllByPurchaseOrderId(UUID id);
    Optional<ReceivingRequest> findByIdAndOrganizationId(UUID id, UUID org);
    List<ReceivingRequest> findAllByOrganizationIdOrderByCreatedAtDesc(UUID org);

    /**
     * The receiving work list deliberately returns scalar columns.  Loading a
     * ReceivingOperationItem entity here makes Hibernate follow its eager
     * many-to-one graph (operation -> request -> purchase order -> organization
     * and employee), which can exceed MySQL's 61-table join limit.
     */
    @Query(value = """
            select rr.id as requestId, rr.request_number as requestNumber,
                   po.id as purchaseOrderId, po.number as purchaseOrderNumber,
                   rr.destination_site_id as warehouseId, rr.destination_site_id as deliveryLocationId,
                   rr.status as requestStatus,
                   rri.id as requestItemId, poi.id as purchaseOrderItemId,
                   p.id as productId, coalesce(p.name, pri.product_name) as productName,
                   pri.sku as sku, coalesce(p.product_type, poi.product_type) as productType,
                   poi.unit as baseUnit, rri.expected_quantity as expectedQuantity,
                   coalesce(sum(roi.received_quantity), 0) as receivedQuantity,
                   coalesce(sum(roi.accepted_quantity), 0) as acceptedQuantity,
                   coalesce(sum(roi.rejected_quantity), 0) as rejectedQuantity
            from receiving_requests rr
            join purchase_orders po on po.id = rr.purchase_order_id
            join receiving_request_items rri on rri.receiving_request_id = rr.id
            join purchase_order_items poi on poi.id = rri.purchase_order_item_id
            join purchase_request_items pri on pri.id = poi.purchase_request_item_id
            left join products p on p.id = pri.global_product_id
            left join receiving_operation_items roi on roi.receiving_request_item_id = rri.id
            where rr.organization_id = :organizationId
              and (:status is null or rr.status = :status)
              and (:warehouseId is null or rr.destination_site_id = :warehouseId)
              and (:purchaseOrderNumber is null or po.number like concat('%', :purchaseOrderNumber, '%'))
              and (:requestId is null or rr.id = :requestId)
            group by rr.id, rr.request_number, po.id, po.number, rr.destination_site_id,
                     rr.destination_site_id, rr.status, rr.created_at, rri.id, poi.id,
                     p.id, p.name, pri.product_name, pri.sku, p.product_type,
                     poi.product_type, poi.unit, rri.expected_quantity
            order by rr.created_at desc, rri.created_at asc
            """, nativeQuery = true)
    List<ReceivingRequestListRow> findListRows(
            @Param("organizationId") UUID organizationId,
            @Param("status") String status,
            @Param("warehouseId") UUID warehouseId,
            @Param("purchaseOrderNumber") String purchaseOrderNumber,
            @Param("requestId") UUID requestId);

    interface ReceivingRequestListRow {
        Object getRequestId(); String getRequestNumber(); Object getPurchaseOrderId(); String getPurchaseOrderNumber();
        Object getWarehouseId(); Object getDeliveryLocationId(); String getRequestStatus(); Object getRequestItemId();
        Object getPurchaseOrderItemId(); Object getProductId(); String getProductName(); String getSku();
        String getProductType(); String getBaseUnit(); BigDecimal getExpectedQuantity(); BigDecimal getReceivedQuantity();
        BigDecimal getAcceptedQuantity(); BigDecimal getRejectedQuantity();
    }
}
