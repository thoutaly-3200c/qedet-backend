package q.qedet.sales.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.employee.entity.Employee;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.entity.OrganizationSite;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "sales_pos_quotes", uniqueConstraints = @UniqueConstraint(name = "uk_pos_quote_token_hash", columnNames = "token_hash"))
@Getter @Setter @NoArgsConstructor
public class PosQuote extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @ManyToOne(optional = false) @JoinColumn(name = "cashier_id", nullable = false) private Employee cashier;
    @ManyToOne(optional = false) @JoinColumn(name = "sales_hall_id", nullable = false) private OrganizationSite salesHall;
    @Column(name = "token_hash", nullable = false, length = 64) private String tokenHash;
    @Column(nullable = false) private Instant expiresAt;
    @Column(nullable = false, length = 3) private String currency;
    @Column(nullable = false, precision = 7, scale = 4) private BigDecimal taxRate;
    @Column(nullable = false) private boolean pricesTaxInclusive;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal subtotal;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal discountTotal;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal taxTotal;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal total;
    @OneToMany(
            mappedBy = "quote",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("sortOrder ASC") private List<PosQuoteLine> lines = new ArrayList<>();
    @OneToOne(mappedBy = "quote") private PosSale sale;
}
