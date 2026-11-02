package q.qedet.warehouse.entity;
import jakarta.persistence.*; import lombok.*; import q.qedet.common.model.TimestampedEntity; import q.qedet.procurement.entity.PurchaseOrderItem; import java.math.BigDecimal;
@Entity @Table(name="receiving_request_items") @Getter @Setter @NoArgsConstructor public class ReceivingRequestItem extends TimestampedEntity {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="receiving_request_id") private ReceivingRequest receivingRequest; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="purchase_order_item_id") private PurchaseOrderItem purchaseOrderItem;
 @Column(nullable=false,precision=19,scale=4) private BigDecimal expectedQuantity=BigDecimal.ZERO; @Version private Long version;
}
