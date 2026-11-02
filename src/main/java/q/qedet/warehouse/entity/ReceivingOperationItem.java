package q.qedet.warehouse.entity;
import jakarta.persistence.*; import lombok.*; import q.qedet.common.model.TimestampedEntity; import q.qedet.product.entity.Product; import java.math.BigDecimal; import java.util.*;
@Entity @Table(name="receiving_operation_items") @Getter @Setter @NoArgsConstructor public class ReceivingOperationItem extends TimestampedEntity {
 public enum ProductResolution { PENDING_PRODUCT, LINKED_EXISTING_PRODUCT, CREATE_WITH_SUPPLIER_QR, CREATE_WITH_NEW_QEDET_QR }
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="receiving_operation_id") private ReceivingOperation receivingOperation; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="receiving_request_item_id") private ReceivingRequestItem receivingRequestItem; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="product_id") private Product product;
 @Column(nullable=false,precision=19,scale=4) private BigDecimal receivedQuantity=BigDecimal.ZERO; @Column(nullable=false,precision=19,scale=4) private BigDecimal acceptedQuantity=BigDecimal.ZERO; @Column(nullable=false,precision=19,scale=4) private BigDecimal rejectedQuantity=BigDecimal.ZERO; private String notes; private String supplierQrIdentifier;
 @Enumerated(EnumType.STRING) @Column(name="product_resolution", nullable=false, length=40) private ProductResolution productResolution=ProductResolution.PENDING_PRODUCT; @Version private Long version;
 @OneToMany(mappedBy="receivingOperationItem",cascade=CascadeType.ALL,orphanRemoval=true) private List<ReceivingOperationZoneAssignment> zoneAssignments=new ArrayList<>();
}
