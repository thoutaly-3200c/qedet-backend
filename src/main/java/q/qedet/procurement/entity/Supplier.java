package q.qedet.procurement.entity;

import jakarta.persistence.*;
import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.organization.entity.Organization;
import java.math.BigDecimal;

@Entity @Table(name = "suppliers")
@Getter @Setter @NoArgsConstructor
public class Supplier extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @Column(nullable = false) private String name;
    private String contact; private String email; private String phone; private String category;
    private String city; private String country; private String address; private String paymentTerms;
    private Integer leadTimeDays; private String currency = "SAR";
    @Column(precision = 5, scale = 2) private BigDecimal rating = BigDecimal.ZERO;
    @Column(precision = 5, scale = 2) private BigDecimal onTimeRate = BigDecimal.ZERO;
    @Column(precision = 5, scale = 2) private BigDecimal defectRate = BigDecimal.ZERO;
    private Integer totalOrders = 0;
    @Column(precision = 14, scale = 2) private BigDecimal spend = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private SupplierStatus status = SupplierStatus.Active;
    public enum SupplierStatus { Active, Inactive }
}
