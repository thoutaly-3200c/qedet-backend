package q.qedet.procurement.entity;

import jakarta.persistence.*;
import lombok.*;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.organization.entity.Organization;
import q.qedet.warehouse.entity.WarehouseReturn;
import java.time.Instant;
import java.util.*;

@Entity @Table(name="supplier_return_operations", uniqueConstraints=@UniqueConstraint(name="uk_supplier_return_reference",columnNames={"organization_id","reference"}))
@Getter @Setter @NoArgsConstructor
public class SupplierReturnOperation extends TimestampedEntity {
 public enum Status { APPROVED, PARTIALLY_DISPATCHED, DISPATCHED, PARTIALLY_ACCEPTED, ACCEPTED, SETTLEMENT_PENDING, SETTLED, CLOSED }
 public enum SettlementType { REFUND, CREDIT_NOTE, REPLACEMENT }
 @ManyToOne(optional=false) @JoinColumn(name="organization_id") private Organization organization;
 @Column(nullable=false,length=64) private String reference;
 @ManyToOne(optional=false) @JoinColumn(name="warehouse_return_id") private WarehouseReturn warehouseReturn;
 @ManyToOne(optional=false) @JoinColumn(name="supplier_id") private Supplier supplier;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=32) private Status status=Status.APPROVED;
 @Enumerated(EnumType.STRING) @Column(length=24) private SettlementType settlementType;
 @Column(length=160) private String settlementReference;
 @Column(length=2000) private String settlementNotes;
 private UUID financialRequestId;
 private Instant approvedAt;
 private Instant dispatchedAt;
 private Instant supplierRespondedAt;
 private Instant settlementRequestedAt;
 private Instant closedAt;
 @Column(length=2000) private String notes;
 @Version private Long version;
 @OneToMany(mappedBy="operation",cascade=CascadeType.ALL,orphanRemoval=true) private List<SupplierReturnOperationItem> items=new ArrayList<>();
}
