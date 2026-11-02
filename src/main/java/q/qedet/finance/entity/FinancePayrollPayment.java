package q.qedet.finance.entity;
import jakarta.persistence.*; import lombok.*; import q.qedet.common.model.TimestampedEntity; import q.qedet.organization.entity.Organization; import java.math.BigDecimal; import java.time.Instant; import java.util.UUID;
@Entity @Table(name = "finance_payroll_payments", uniqueConstraints = @UniqueConstraint(name = "uk_finance_payroll_payment_operation", columnNames = "operation_id"), indexes = @Index(name = "idx_finance_payroll_payment_payroll", columnList = "organization_id,payroll_id"))
@Getter @Setter @NoArgsConstructor public class FinancePayrollPayment extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization; @Column(nullable = false) private UUID payrollId; @Column(nullable = false) private UUID employeeId;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount; @Column(nullable = false, length = 3) private String currency; @Column(nullable = false) private UUID sourceAccountId; @Column(nullable = false) private UUID operationId;
    @Column(length = 20) private String receiverVerificationStatus; @Column(nullable = false) private UUID financeActorId; @Column(nullable = false) private Instant paidAt;
}
