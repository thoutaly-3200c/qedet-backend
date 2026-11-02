package q.qedet.procurement.entity;
import jakarta.persistence.*;
import lombok.*;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.organization.entity.OrganizationZone;
import java.math.BigDecimal;
@Entity @Table(name="supplier_return_dispatch_items") @Getter @Setter @NoArgsConstructor
public class SupplierReturnDispatchItem extends TimestampedEntity {
 @ManyToOne(optional=false) @JoinColumn(name="dispatch_id") private SupplierReturnDispatch dispatch;
 @ManyToOne(optional=false) @JoinColumn(name="operation_item_id") private SupplierReturnOperationItem operationItem;
 @ManyToOne(optional=false) @JoinColumn(name="source_zone_id") private OrganizationZone sourceZone;
 @Column(nullable=false,precision=19,scale=4) private BigDecimal quantity;
}
