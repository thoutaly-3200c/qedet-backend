package q.qedet.sales.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;

import java.math.BigDecimal;
import java.util.UUID;

@Entity @Table(name = "sales_stock_request_items") @Getter @Setter @NoArgsConstructor
public class SalesStockRequestItem extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "sales_stock_request_id") private SalesStockRequest request;
    @Column(nullable = false) private UUID productId;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal requestedQuantity;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal approvedQuantity = BigDecimal.ZERO;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal receivedQuantity = BigDecimal.ZERO;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal fulfilledQuantity = BigDecimal.ZERO;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal pendingAcceptanceQuantity = BigDecimal.ZERO;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal acceptedQuantity = BigDecimal.ZERO;
}
