package q.qedet.finance.entity;

import jakarta.persistence.*;
import lombok.*;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.employee.entity.Employee;
import q.qedet.finance.model.CashTransactionType;
import q.qedet.organization.entity.Organization;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "finance_cash_transactions", indexes = {
        @Index(name = "idx_finance_cash_transaction_box_posted", columnList = "cash_box_id,posted_at"),
        @Index(name = "idx_finance_cash_transaction_source", columnList = "organization_id,source_type,source_id")
})
@Getter @Setter @NoArgsConstructor
public class CashTransaction extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @ManyToOne(optional = false) @JoinColumn(name = "cash_box_id", nullable = false) private CashBox cashBox;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private CashTransactionType type;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 3) private String currency;
    @Column(nullable = false, length = 60) private String sourceType;
    @Column(nullable = false) private UUID sourceId;
    @Column(length = 160) private String reference;
    @Column(length = 2000) private String description;
    @ManyToOne(optional = false) @JoinColumn(name = "created_by_employee_id", nullable = false) private Employee createdBy;
    @ManyToOne @JoinColumn(name = "approved_by_employee_id") private Employee approvedBy;
    @Column(name = "posted_at", nullable = false) private Instant postedAt;
    @Column(nullable = false) private Instant completedAt;
    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2) private BigDecimal balanceAfter;
    private UUID transferId;
}
