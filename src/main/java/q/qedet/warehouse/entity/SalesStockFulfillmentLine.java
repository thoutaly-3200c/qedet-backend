package q.qedet.warehouse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity @Table(name = "sales_stock_fulfillment_lines") @Getter @Setter @NoArgsConstructor
public class SalesStockFulfillmentLine extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "fulfillment_id", nullable = false) private SalesStockFulfillment fulfillment;
    @Column(nullable = false) private UUID productId;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal quantity;
    @OneToMany(mappedBy = "line", cascade = CascadeType.ALL, orphanRemoval = true) private List<SalesStockFulfillmentAllocation> allocations = new ArrayList<>();
}
