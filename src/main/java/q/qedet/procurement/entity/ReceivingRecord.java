package q.qedet.procurement.entity;
import jakarta.persistence.*; import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;
import q.qedet.common.model.TimestampedEntity; import q.qedet.organization.entity.Organization;
import java.time.LocalDate; import java.util.UUID;
@Entity @Table(name="receiving_records")
@Getter @Setter @NoArgsConstructor
public class ReceivingRecord extends TimestampedEntity {
 @ManyToOne(optional=false) @JoinColumn(name="organization_id",nullable=false) private Organization organization;
 @Column(nullable=false,unique=true) private String number; private UUID purchaseOrderId; private String supplierName; private LocalDate expectedDate; private LocalDate receivedDate;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private ReceivingStatus status=ReceivingStatus.Pending;
 public enum ReceivingStatus { Pending, In_Progress, Inspecting, Completed }
}
