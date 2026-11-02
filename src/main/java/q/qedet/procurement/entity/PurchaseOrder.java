package q.qedet.procurement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.employee.entity.Employee;
import q.qedet.organization.entity.Organization;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_orders", uniqueConstraints = @UniqueConstraint(
        name = "uk_purchase_order_org_number", columnNames = {"organization_id", "number"}))
@Getter @Setter @NoArgsConstructor
public class PurchaseOrder extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @Column(nullable = false, length = 64) private String number;
    @ManyToOne(optional = false) @JoinColumn(name = "purchase_request_id", nullable = false)
    private PurchaseRequest purchaseRequest;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16)
    private Status status = Status.DRAFT;
    @Column(length = 2000) private String notes;
    @ManyToOne(optional = false) @JoinColumn(name = "created_by", nullable = false)
    private Employee createdBy;
    @Version private Long version;
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseOrderItem> items = new ArrayList<>();

    public enum Status { DRAFT, CONFIRMED, CLOSED, CANCELLED }
}
