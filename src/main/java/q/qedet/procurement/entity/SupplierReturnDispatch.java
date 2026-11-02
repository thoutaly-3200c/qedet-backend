package q.qedet.procurement.entity;
import jakarta.persistence.*;
import lombok.*;
import q.qedet.common.model.TimestampedEntity;
import java.time.Instant;
import java.util.*;
@Entity @Table(name="supplier_return_dispatches",uniqueConstraints=@UniqueConstraint(name="uk_supplier_return_dispatch_ref",columnNames={"operation_id","reference"}))
@Getter @Setter @NoArgsConstructor public class SupplierReturnDispatch extends TimestampedEntity {
 @ManyToOne(optional=false) @JoinColumn(name="operation_id") private SupplierReturnOperation operation;
 @Column(nullable=false,length=80) private String reference;
 @Column(length=160) private String trackingReference;
 @Column(nullable=false) private UUID employeeId;
 @Column(nullable=false) private Instant dispatchedAt;
 @OneToMany(mappedBy="dispatch",cascade=CascadeType.ALL,orphanRemoval=true) private List<SupplierReturnDispatchItem> items=new ArrayList<>();
}
