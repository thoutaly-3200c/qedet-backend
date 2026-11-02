package q.qedet.finance.entity;

import jakarta.persistence.*;
import lombok.*;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.finance.model.FinanceWorkflowTypes.RequestStatus;
import q.qedet.finance.model.FinanceWorkflowTypes.RequestFulfillmentStatus;
import q.qedet.organization.entity.Organization;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

@Entity @Table(name = "finance_requests", indexes = {
        @Index(name = "idx_finance_request_org_status", columnList = "organization_id,status"),
        @Index(name = "idx_finance_request_source", columnList = "organization_id,source_domain,source_record_id")})
@Getter @Setter @NoArgsConstructor
public class FinanceRequest extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @Column(name = "source_domain", nullable = false, length = 60) private String sourceDomain;
    @Column(name = "source_record_id", nullable = false, length = 160) private String sourceRecordId;
    @Column(name = "request_number", length = 64) private String requestNumber;
    @Column(name = "request_type", nullable = false, length = 60) private String requestType;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @Column(name = "approved_amount", precision = 19, scale = 2) private BigDecimal approvedAmount;
    @Column(nullable = false, length = 3) private String currency;
    @Column(nullable = false, length = 2000) private String reason;
    @Column(length = 4000) private String notes;
    @Column(length = 500) private String supportingReference;
    private LocalDate dueDate;
    @Column(name = "requester_id", nullable = false) private UUID requesterId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private RequestStatus status = RequestStatus.PENDING;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private RequestFulfillmentStatus fulfillmentStatus = RequestFulfillmentStatus.UNPAID;
    private UUID reviewerId; private Instant reviewedAt; @Column(length = 2000) private String rejectionReason;
    private UUID cancelledBy; private Instant cancelledAt; @Column(length = 2000) private String cancellationReason;
    private UUID resultingOperationId;
}
