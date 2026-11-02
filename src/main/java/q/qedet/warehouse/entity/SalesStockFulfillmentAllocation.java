package q.qedet.warehouse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.organization.entity.OrganizationZone;
import java.math.BigDecimal;

@Entity @Table(name = "sales_stock_fulfillment_allocations") @Getter @Setter @NoArgsConstructor
public class SalesStockFulfillmentAllocation extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "fulfillment_line_id", nullable = false) private SalesStockFulfillmentLine line;
    @ManyToOne(optional = false) @JoinColumn(name = "source_zone_id", nullable = false) private OrganizationZone sourceZone;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal quantity;
}
