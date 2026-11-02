package q.qedet.warehouse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.entity.OrganizationZone;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "stock_movements", indexes = {
        @Index(name = "idx_movement_org_product", columnList = "organization_id,product_id")
})
@Getter
@Setter
@NoArgsConstructor
public class StockMovement extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @Column(nullable = false) private UUID productId;
    @ManyToOne @JoinColumn(name = "from_organization_zone_id") private OrganizationZone fromZone;
    @ManyToOne @JoinColumn(name = "to_organization_zone_id") private OrganizationZone toZone;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal quantity = BigDecimal.ZERO;
    @Column(nullable = false) private String movementType;
    private String reference;
    @Column(length = 1000) private String notes;
}
