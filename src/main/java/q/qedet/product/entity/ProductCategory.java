package q.qedet.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;

@Entity(name = "GlobalProductCategory")
@Table(
        name = "product_categories",
        indexes = {
                @Index(name = "idx_product_categories_parent_id", columnList = "parent_id"),
                @Index(name = "idx_product_categories_name", columnList = "name")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ProductCategory extends TimestampedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ProductCategory parent;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;
}
