package q.qedet.finance.entity;
import jakarta.persistence.*; import lombok.*; import q.qedet.common.model.TimestampedEntity; import q.qedet.finance.model.FinanceWorkflowTypes.LedgerDirection; import q.qedet.organization.entity.Organization; import java.math.BigDecimal; import java.time.Instant; import java.util.UUID;
@Entity @Table(name = "finance_ledger_entries", indexes = {@Index(name = "idx_finance_ledger_account_posted", columnList = "account_id,posted_at"), @Index(name = "idx_finance_ledger_operation", columnList = "operation_id")})
@Getter @Setter @NoArgsConstructor public class FinanceLedgerEntry extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization; @Column(nullable = false) private UUID operationId; @Column(nullable = false) private UUID accountId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private LedgerDirection direction; @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount; @Column(nullable = false, length = 3) private String currency;
    @Column(nullable = false) private Instant postedAt; @Column(length = 160) private String reference;
}
