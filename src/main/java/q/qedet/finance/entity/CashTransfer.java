package q.qedet.finance.entity;

import jakarta.persistence.*;
import lombok.*;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.employee.entity.Employee;
import q.qedet.finance.model.CashTransferStatus;
import q.qedet.organization.entity.Organization;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "finance_cash_transfers", indexes = @Index(name = "idx_finance_transfer_org_status", columnList = "organization_id,status"))
@Getter @Setter @NoArgsConstructor
public class CashTransfer extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @ManyToOne(optional = false) @JoinColumn(name = "from_cash_box_id", nullable = false) private CashBox fromCashBox;
    @ManyToOne(optional = false) @JoinColumn(name = "to_cash_box_id", nullable = false) private CashBox toCashBox;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 3) private String currency;
    @Column(nullable = false, length = 2000) private String reason;
    @ManyToOne(optional = false) @JoinColumn(name = "requested_by_employee_id", nullable = false) private Employee requestedBy;
    @ManyToOne @JoinColumn(name = "approved_by_employee_id") private Employee approvedBy;
    @ManyToOne @JoinColumn(name = "executed_by_employee_id") private Employee executedBy;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private CashTransferStatus status = CashTransferStatus.PENDING_APPROVAL;
    private Instant executedAt;
}
