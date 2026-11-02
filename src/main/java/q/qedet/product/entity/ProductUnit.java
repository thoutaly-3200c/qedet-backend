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

@Entity(name = "GlobalProductUnit")
@Table(
        name = "product_units",
        indexes = {
                @Index(name = "idx_product_units_name", columnList = "name"),
                @Index(name = "idx_product_units_symbol", columnList = "symbol")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_product_units_name", columnNames = "name"),
                @UniqueConstraint(name = "uk_product_units_symbol", columnNames = "symbol")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ProductUnit extends TimestampedEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String symbol;
}
