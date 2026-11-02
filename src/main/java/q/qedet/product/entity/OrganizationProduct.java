package q.qedet.product.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.organization.entity.Organization;

import java.util.ArrayList;
import java.util.List;

@Entity(name = "GlobalOrganizationProduct")
@Table(
        name = "organization_products",
        indexes = {
                @Index(name = "idx_organization_products_organization_id", columnList = "organization_id"),
                @Index(name = "idx_organization_products_product_id", columnList = "product_id")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_organization_products_org_product", columnNames = {"organization_id", "product_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class OrganizationProduct extends TimestampedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(length = 100)
    private String organizationSku;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrganizationProductStatus status = OrganizationProductStatus.ACTIVE;

    @OneToMany(mappedBy = "organizationProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductBarcode> barcodes = new ArrayList<>();
}
