package q.qedet.warehouse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.organization.entity.OrganizationZone;

import java.math.BigDecimal;

@Entity
@Table(name = "receiving_operation_zone_assignments", uniqueConstraints =
        @UniqueConstraint(name = "uk_receiving_operation_item_zone", columnNames = {"receiving_operation_item_id", "organization_zone_id"}))
@Getter
@Setter
@NoArgsConstructor
public class ReceivingOperationZoneAssignment extends TimestampedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiving_operation_item_id")
    private ReceivingOperationItem receivingOperationItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_zone_id", nullable = false)
    private OrganizationZone zone;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal acceptedQuantity = BigDecimal.ZERO;
}
