package q.qedet.warehouse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.organization.entity.Organization;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Physical warehouse dispatch for one Sales stock request. */
@Entity
@Table(name = "sales_stock_fulfillments", indexes = @Index(name = "idx_sales_fulfillment_request", columnList = "sales_stock_request_id"))
@Getter @Setter @NoArgsConstructor
public class SalesStockFulfillment extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @Column(name = "sales_stock_request_id", nullable = false) private UUID salesStockRequestId;
    @Column(name = "warehouse_id", nullable = false) private UUID warehouseId;
    @Column(name = "sales_hall_id", nullable = false) private UUID salesHallId;
    @ManyToOne(optional = false) @JoinColumn(name = "destination_zone_id", nullable = false) private q.qedet.organization.entity.OrganizationZone destinationZone;
    @Column(nullable = false, length = 32) private String status = "CONFIRMED";
    @Column(nullable = false) private UUID confirmedBy;
    private UUID decidedBy;
    @Column(length = 1000) private String decisionNotes;
    @OneToMany(mappedBy = "fulfillment", cascade = CascadeType.ALL, orphanRemoval = true) private List<SalesStockFulfillmentLine> lines = new ArrayList<>();
}
