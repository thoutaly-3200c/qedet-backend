package q.qedet.warehouse.entity;
import jakarta.persistence.*; import lombok.*; import q.qedet.common.model.TimestampedEntity; import q.qedet.organization.entity.*; import q.qedet.procurement.entity.PurchaseOrder; import java.util.*;
@Entity @Table(name="receiving_requests") @Getter @Setter @NoArgsConstructor public class ReceivingRequest extends TimestampedEntity {
 public enum Status { PENDING, IN_PROGRESS, PARTIALLY_RECEIVED, COMPLETED, CANCELLED }
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="organization_id") private Organization organization; @Column(nullable=false) private String requestNumber;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="purchase_order_id") private PurchaseOrder purchaseOrder; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="destination_site_id") private OrganizationSite destinationLocation;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private Status status=Status.PENDING; @Version private Long version;
 @OneToMany(mappedBy="receivingRequest",cascade=CascadeType.ALL,orphanRemoval=true) private List<ReceivingRequestItem> items=new ArrayList<>();
}
