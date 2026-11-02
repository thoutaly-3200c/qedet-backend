package q.qedet.finance.entity;

import jakarta.persistence.*;
import lombok.*;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.employee.entity.Employee;
import q.qedet.finance.model.*;
import q.qedet.organization.entity.Organization;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "finance_payment_requests",
        uniqueConstraints = @UniqueConstraint(name = "uk_finance_payment_source", columnNames = {"organization_id", "request_type", "source_reference"}),
        indexes = @Index(name = "idx_finance_payment_request_org_status", columnList = "organization_id,status"))
@Getter @Setter @NoArgsConstructor
public class PaymentRequest extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @Enumerated(EnumType.STRING) @Column(name = "request_type", nullable = false, length = 32) private PaymentRequestType requestType;
    @Column(nullable = false, length = 100) private String sourceDepartment;
    @Column(name = "source_reference", nullable = false, length = 160) private String sourceReference;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 3) private String currency;
    @ManyToOne(optional = false) @JoinColumn(name = "requested_by_employee_id", nullable = false) private Employee requestedBy;
    @Column(nullable = false) private Instant requestedAt;
    @Column(nullable = false, length = 2000) private String reason;
    @Column(length = 4000) private String supportingDocuments;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PaymentRequestStatus status = PaymentRequestStatus.RECEIVED;
    @ManyToOne @JoinColumn(name = "reviewed_by_employee_id") private Employee reviewedBy;
    private Instant reviewedAt;
    @Column(length = 2000) private String rejectionReason;
    private Instant completedAt;
    private UUID supplierId;
    private UUID purchaseOrderId;
    private UUID purchaseOrderItemId;
    @Column(length = 160) private String supplierInvoice;
    private LocalDate dueDate;
}
