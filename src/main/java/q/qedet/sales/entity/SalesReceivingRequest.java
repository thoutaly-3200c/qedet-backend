package q.qedet.sales.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.entity.OrganizationZone;
import q.qedet.warehouse.entity.SalesStockFulfillment;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sales_receiving_requests", uniqueConstraints =
        @UniqueConstraint(name = "uk_sales_receiving_fulfillment", columnNames = "fulfillment_id"))
@Getter @Setter @NoArgsConstructor
public class SalesReceivingRequest extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @ManyToOne(optional = false) @JoinColumn(name = "sales_stock_request_id", nullable = false) private SalesStockRequest stockRequest;
    @OneToOne(optional = false) @JoinColumn(name = "fulfillment_id", nullable = false) private SalesStockFulfillment fulfillment;
    @Column(name = "sales_hall_id", nullable = false) private UUID salesHallId;
    @ManyToOne(optional = false) @JoinColumn(name = "destination_zone_id", nullable = false) private OrganizationZone destinationZone;
    @Column(nullable = false, length = 32) private String status = "PENDING";
    private UUID acceptedBy;
    @Column(length = 1000) private String notes;
    @OneToMany(mappedBy = "receivingRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SalesReceivingItem> items = new ArrayList<>();
}
