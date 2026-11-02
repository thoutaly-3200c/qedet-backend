package q.qedet.warehouse.entity;
import jakarta.persistence.*; import lombok.*; import q.qedet.common.model.TimestampedEntity; import q.qedet.organization.entity.Organization; import q.qedet.employee.entity.Employee; import java.time.Instant; import java.util.*;
@Entity @Table(name="receiving_operations") @Getter @Setter @NoArgsConstructor public class ReceivingOperation extends TimestampedEntity {
 public enum Status { IN_PROGRESS, COMPLETED, CANCELLED }
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="organization_id") private Organization organization; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="receiving_request_id") private ReceivingRequest receivingRequest;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="warehouse_employee_id") private Employee warehouseEmployee; @Enumerated(EnumType.STRING) @Column(nullable=false) private Status status=Status.IN_PROGRESS; private Instant startedAt; private Instant completedAt; private String notes; private boolean inventoryApplied; @Version private Long version;
 @OneToMany(mappedBy="receivingOperation",cascade=CascadeType.ALL,orphanRemoval=true) private List<ReceivingOperationItem> items=new ArrayList<>();
}
