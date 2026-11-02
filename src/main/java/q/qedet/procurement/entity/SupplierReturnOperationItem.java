package q.qedet.procurement.entity;

import jakarta.persistence.*;
import lombok.*;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.warehouse.entity.*;
import java.math.BigDecimal;

@Entity @Table(name="supplier_return_operation_items",uniqueConstraints=@UniqueConstraint(name="uk_supplier_return_request_item",columnNames="warehouse_return_item_id"))
@Getter @Setter @NoArgsConstructor
public class SupplierReturnOperationItem extends TimestampedEntity {
 @ManyToOne(optional=false) @JoinColumn(name="operation_id") private SupplierReturnOperation operation;
 @ManyToOne(optional=false) @JoinColumn(name="warehouse_return_item_id") private WarehouseReturnItem warehouseReturnItem;
 @ManyToOne(optional=false) @JoinColumn(name="purchase_order_item_id") private PurchaseOrderItem purchaseOrderItem;
 @ManyToOne(optional=false) @JoinColumn(name="receiving_operation_item_id") private ReceivingOperationItem receivingOperationItem;
 @Column(nullable=false,precision=19,scale=4) private BigDecimal approvedQuantity;
 @Column(nullable=false,precision=19,scale=4) private BigDecimal dispatchedQuantity=BigDecimal.ZERO;
 @Column(nullable=false,precision=19,scale=4) private BigDecimal supplierAcceptedQuantity=BigDecimal.ZERO;
 @Column(nullable=false,precision=19,scale=4) private BigDecimal supplierRejectedQuantity=BigDecimal.ZERO;
 @Column(nullable=false,precision=19,scale=4) private BigDecimal unitCost;
 @Column(nullable=false,length=3) private String currency;
}
