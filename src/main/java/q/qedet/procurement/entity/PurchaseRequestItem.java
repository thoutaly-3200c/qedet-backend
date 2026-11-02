package q.qedet.procurement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.product.entity.Product;
import java.math.BigDecimal;

@Entity
@Table(name = "purchase_request_items")
@Getter @Setter @NoArgsConstructor
public class PurchaseRequestItem extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "purchase_request_id", nullable = false)
    private PurchaseRequest purchaseRequest;
    /**
     * The legacy product_id column remains available for historical migration.
     * New workflow code uses the global product identity exclusively.
     */
    @ManyToOne @JoinColumn(name = "global_product_id") private Product product;
    @Column(nullable = false, length = 255) private String productName;
    private String sku;
    @Column(length = 120) private String itemCategory;
    @Column(length = 2000) private String description;
    @Column(precision = 14, scale = 2, nullable = false) private BigDecimal qty;
    @Column(length = 64) private String unit;
    @Column(precision = 14, scale = 2) private BigDecimal unitPrice;
    @Column(length = 3) private String currency;
    @Column(length = 2000) private String notes;
}
