package q.qedet.sales.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.organization.entity.Organization;

import java.math.BigDecimal;
import java.util.UUID;

@Entity @Table(name = "sales_packages") @Getter @Setter @NoArgsConstructor
public class SalesPackage extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id") private Organization organization;
    @Column(nullable = false) private UUID salesHallId;
    @Column(nullable = false) private UUID productId;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal weight;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal unitPrice;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal totalPrice;
    @Column(nullable = false) private String status = "ACTIVE";
    private String qrReference;
    @Column(nullable = false) private UUID createdBy;
}
