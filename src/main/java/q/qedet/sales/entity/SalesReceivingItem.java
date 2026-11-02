package q.qedet.sales.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;

import java.math.BigDecimal;
import java.util.UUID;

@Entity @Table(name = "sales_receiving_items") @Getter @Setter @NoArgsConstructor
public class SalesReceivingItem extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "receiving_request_id", nullable = false)
    private SalesReceivingRequest receivingRequest;
    @Column(nullable = false) private UUID productId;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal dispatchedQuantity;
    @Column(precision = 14, scale = 2) private BigDecimal actualReceivedQuantity;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal discrepancyQuantity = BigDecimal.ZERO;
    @Column(name = "accepted_zone_id") private UUID acceptedZoneId;
    @Column(name = "rejected_zone_id") private UUID rejectedZoneId;
}
