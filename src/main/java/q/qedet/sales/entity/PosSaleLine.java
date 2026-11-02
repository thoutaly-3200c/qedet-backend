package q.qedet.sales.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;
import java.math.BigDecimal;
import java.util.UUID;

@Entity @Table(name = "sales_pos_sale_lines") @Getter @Setter @NoArgsConstructor
public class PosSaleLine extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "sale_id", nullable = false) private PosSale sale;
    @Column(nullable = false) private UUID productId;
    private UUID packageId;
    @Column(length = 160) private String serialNumber;
    @Column(nullable = false, length = 200) private String productName;
    @Column(length = 100) private String sku;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal quantity;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal unitPrice;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal lineTotal;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal taxAmount;
}
