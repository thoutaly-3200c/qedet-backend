package q.qedet.finance.entity;
import jakarta.persistence.*; import lombok.*; import q.qedet.common.model.TimestampedEntity; import q.qedet.finance.model.FinanceWorkflowTypes.*; import q.qedet.organization.entity.Organization; import java.math.BigDecimal; import java.time.Instant; import java.util.UUID;
@Entity @Table(name = "finance_operations", uniqueConstraints = @UniqueConstraint(name = "uk_finance_operation_idempotency", columnNames = {"organization_id","idempotency_key"}), indexes = @Index(name = "idx_finance_operation_org_status", columnList = "organization_id,status"))
@Getter @Setter @NoArgsConstructor public class FinanceOperation extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private OperationType type;
    private UUID sourceAccountId; private UUID destinationAccountId; @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount; @Column(nullable = false, length = 3) private String currency;
    @Column(length = 160) private String reference; @Column(length = 2000) private String description; @Column(length = 4000) private String notes; private UUID financeRequestId;
    @Column(name = "created_by", nullable = false) private UUID createdBy; @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private OperationStatus status = OperationStatus.PENDING;
    @Enumerated(EnumType.STRING) @Column(name = "evidence_status", nullable = false, length = 20)
    private EvidenceStatus evidenceStatus = EvidenceStatus.NOT_REQUIRED;
    private UUID evidenceVerifiedBy; private Instant evidenceVerifiedAt; @Column(length = 2000) private String evidenceRejectionReason;
    private Instant executedAt; private Instant confirmedAt; @Column(name = "idempotency_key", nullable = false, length = 128) private String idempotencyKey;
}
