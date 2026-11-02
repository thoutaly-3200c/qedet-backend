package q.qedet.sales.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.organization.entity.Organization;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "sales_prices",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_sales_price_org_product",
                columnNames = {"organization_id", "product_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class SalesPrice extends TimestampedEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "current_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal currentPrice;

    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;
}