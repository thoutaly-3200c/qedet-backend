package q.qedet.procurement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.employee.entity.Employee;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "purchase_order_items", indexes = {
        @Index(name = "idx_po_item_request_item", columnList = "purchase_request_item_id"),
        @Index(name = "idx_po_item_supplier", columnList = "supplier_id")
})
@Getter @Setter @NoArgsConstructor
public class PurchaseOrderItem extends TimestampedEntity {
    @ManyToOne(fetch = FetchType.LAZY,optional = false) @JoinColumn(name = "purchase_order_id", nullable = false) private PurchaseOrder purchaseOrder;
    @ManyToOne(fetch = FetchType.LAZY,optional = false) @JoinColumn(name = "purchase_request_item_id", nullable = false) private PurchaseRequestItem purchaseRequestItem;
    @ManyToOne(fetch = FetchType.LAZY,optional = false) @JoinColumn(name = "supplier_id", nullable = false) private Supplier supplier;
    private LocalDate orderDate;
    private LocalDate expectedDeliveryDate;
    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 24)
    private ProductType productType = ProductType.STANDARD_UNIT;
    @Column(nullable = false, length = 3) private String currency;
    @Column(length = 60) private String paymentTerms;
    @Enumerated(EnumType.STRING) @Column(length = 20) private PaymentMethod paymentMethod;
    @Enumerated(EnumType.STRING) @Column(length = 20) private PaymentTiming paymentTiming;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal quantity;
    @Column(length = 64) private String unit;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal totalPrice;
    @Column(precision = 7, scale = 4) private BigDecimal discountPercent = BigDecimal.ZERO;
    @Column(precision = 19, scale = 2) private BigDecimal discountAmount = BigDecimal.ZERO;
    @Column(precision = 19, scale = 2) private BigDecimal taxAmount = BigDecimal.ZERO;
    @Column(precision = 19, scale = 2) private BigDecimal shippingAmount = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private OrderStatus orderStatus = OrderStatus.DRAFT;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private ReceivingStatus receivingStatus = ReceivingStatus.NOT_STARTED;
    @ManyToOne(fetch = FetchType.LAZY,optional = false) @JoinColumn(name = "created_by", nullable = false) private Employee createdBy;
    @Version private Long version;

    public enum ProductType {
        STANDARD_UNIT,
        SERIALIZED,
        WEIGHTED_BULK,
        MULTI_PACK
    }
    public enum OrderStatus { DRAFT, CONFIRMED, SENT_TO_SUPPLIER, SUPPLIER_CONFIRMED, READY_TO_RECEIVE, CLOSED, CANCELLED }
    public enum ReceivingStatus { NOT_STARTED, RECEIVING, PARTIALLY_RECEIVED, FULLY_RECEIVED }
    public enum PaymentMethod { CASH, CARD, BANK_TRANSFER, CHEQUE, OTHER }
    public enum PaymentTiming { BEFORE_DELIVERY, ON_DELIVERY, AFTER_DELIVERY }
}
