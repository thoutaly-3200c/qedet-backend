package q.qedet.sales.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;

import java.math.BigDecimal;
import java.util.UUID;

@Entity @Table(name = "sales_return_items") @Getter @Setter @NoArgsConstructor
public class SalesReturnItem extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "sales_return_id") private SalesReturn salesReturn;
    @Column(nullable = false) private UUID productId;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal requestedQuantity;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal receivedQuantity = BigDecimal.ZERO;
    private String serialReference;
    private String batchReference;
}
