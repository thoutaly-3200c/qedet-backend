package q.qedet.warehouse.entity;

import jakarta.persistence.*;
import lombok.*;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.organization.entity.Organization;
import java.util.*;

@Entity @Table(name = "warehouse_returns", indexes = {
        @Index(name = "idx_warehouse_return_org_type_status", columnList = "organization_id,type,status")})
@Getter @Setter @NoArgsConstructor
public class WarehouseReturn extends TimestampedEntity {
    public enum Type { SALES_HALL, CUSTOMER, SUPPLIER, DAMAGE }
    public enum Status { PENDING_RECEIPT, RECEIVED, PENDING_INSPECTION, ACCEPTED, REJECTED, QUARANTINE, SUPPLIER_OPEN, PROCUREMENT_APPROVED, PARTIALLY_RETURNED, CLOSED }
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @Column(nullable = false, length = 64) private String reference;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private Type type;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private Status status;
    @Column(nullable = false, length = 2000) private String reason;
    @Column(length = 160) private String salesOrderReference;
    @Column(length = 160) private String receiptReference;
    private UUID receivingOperationId;
    private UUID financialRequestId;
    @OneToMany(mappedBy = "warehouseReturn", cascade = CascadeType.ALL, orphanRemoval = true) private List<WarehouseReturnItem> items = new ArrayList<>();
}
