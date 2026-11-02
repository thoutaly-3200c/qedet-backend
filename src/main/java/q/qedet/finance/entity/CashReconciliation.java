package q.qedet.finance.entity;

import jakarta.persistence.*;
import lombok.*;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.employee.entity.Employee;
import q.qedet.organization.entity.Organization;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "finance_cash_reconciliations", indexes = @Index(name = "idx_finance_reconciliation_box", columnList = "cash_box_id,created_at"))
@Getter @Setter @NoArgsConstructor
public class CashReconciliation extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @ManyToOne(optional = false) @JoinColumn(name = "cash_box_id", nullable = false) private CashBox cashBox;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal systemBalance;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal physicalCount;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal difference;
    @Column(nullable = false, length = 2000) private String reason;
    @Column(length = 4000) private String supportingInformation;
    @ManyToOne(optional = false) @JoinColumn(name = "counted_by_employee_id", nullable = false) private Employee countedBy;
    @ManyToOne(optional = false) @JoinColumn(name = "approved_by_employee_id", nullable = false) private Employee approvedBy;
    @Column(nullable = false) private Instant completedAt;
}
