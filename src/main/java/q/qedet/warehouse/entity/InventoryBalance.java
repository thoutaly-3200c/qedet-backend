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
@Table(name = "inventory_balances", indexes = {
        @Index(name = "idx_inventory_org_product_zone", columnList = "organization_id,product_id,organization_zone_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class InventoryBalance extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @Column(nullable = false) private UUID productId;
    @ManyToOne(optional = false) @JoinColumn(name = "organization_zone_id", nullable = false) private OrganizationZone zone;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal onHand = BigDecimal.ZERO;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal reserved = BigDecimal.ZERO;
    @Version private Long version;

}
