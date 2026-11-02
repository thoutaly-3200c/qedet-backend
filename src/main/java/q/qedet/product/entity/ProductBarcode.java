package q.qedet.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.organization.entity.Organization;
import q.qedet.product.service.BarcodeNormalizer;

@Entity(name = "GlobalProductBarcode")
@Table(
        name = "product_barcodes",
        indexes = {
                @Index(name = "idx_product_barcodes_barcode", columnList = "barcode"),
                @Index(name = "idx_product_barcodes_product_id", columnList = "product_id"),
                @Index(name = "idx_product_barcodes_organization_product_id", columnList = "organization_product_id"),
                @Index(name = "idx_product_barcodes_organization_id", columnList = "organization_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_product_barcodes_barcode", columnNames = "barcode"),
                @UniqueConstraint(name = "uk_product_barcodes_organization_barcode", columnNames = {"organization_id", "barcode"})
        }
)
@Check(constraints = "((product_id IS NOT NULL AND organization_product_id IS NULL) OR (product_id IS NULL AND organization_product_id IS NOT NULL))")
@Getter
@Setter
@NoArgsConstructor
public class ProductBarcode extends TimestampedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_product_id")
    private OrganizationProduct organizationProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(nullable = false, length = 128)
    private String barcode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProductBarcodeType barcodeType = ProductBarcodeType.OTHER;

    @Column(nullable = false)
    private boolean isPrimary = false;

    @PrePersist
    @PreUpdate
    public void normalize() {
        this.barcode = BarcodeNormalizer.normalize(this.barcode);
        if (this.organizationProduct != null) {
            this.organization = this.organizationProduct.getOrganization();
        }
        if (this.product != null && this.organizationProduct == null) {
            this.organization = null;
        }
        if (this.product == null && this.organizationProduct != null) {
            this.organization = this.organizationProduct.getOrganization();
        }
    }
}
