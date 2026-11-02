package q.qedet.sales.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sales_price_history")
@Getter
@Setter
@NoArgsConstructor
public class SalesPriceHistory extends TimestampedEntity {

    @Column(name = "sales_price_id", nullable = false)
    private UUID salesPriceId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "previous_price", precision = 14, scale = 2)
    private BigDecimal previousPrice;

    @Column(name = "new_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal newPrice;

    @Column(name = "changed_by", nullable = false)
    private UUID changedBy;

    @Column(length = 1000)
    private String reason;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;
}