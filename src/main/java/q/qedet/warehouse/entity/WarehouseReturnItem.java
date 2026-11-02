package q.qedet.warehouse.entity;

import jakarta.persistence.*;
import lombok.*;
import q.qedet.common.model.TimestampedEntity;
import java.math.BigDecimal;
import java.util.UUID;

@Entity @Table(name = "warehouse_return_items", indexes = @Index(name = "idx_warehouse_return_item_return", columnList = "warehouse_return_id"))
@Getter @Setter @NoArgsConstructor
public class WarehouseReturnItem extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "warehouse_return_id", nullable = false) private WarehouseReturn warehouseReturn;
    @Column(nullable = false) private UUID productId;
    @Column(length = 80) private String qrReference;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal requestedQuantity;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal receivedQuantity = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal returnedQuantity = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal inspectionQuantity = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal acceptedQuantity = BigDecimal.ZERO;
    @Column(nullable = false, length = 32) private String status = "PENDING";
    private UUID inspectionLocationId;
    /** Source SiteZone for a damage hold; inspectionLocationId remains the inspection SiteZone. */
    private UUID sourceSiteZoneId;
    @Column(length = 512) private String lastInspectionDecisionKey;
    @Column(length = 80) private String conditionCode;
    @Column(length = 2000) private String handlingNotes;
}
