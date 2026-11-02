package q.qedet.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;

@Entity(name = "GlobalProductBrand")
@Table(
        name = "product_brands",
        indexes = @Index(name = "idx_product_brands_name", columnList = "name"),
        uniqueConstraints = @UniqueConstraint(name = "uk_product_brands_name", columnNames = "name")
)
@Getter
@Setter
@NoArgsConstructor
public class ProductBrand extends TimestampedEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;
}
