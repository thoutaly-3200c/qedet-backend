package q.qedet.finance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "finance_request_details")
@Getter @Setter @NoArgsConstructor
public class FinanceRequestDetail extends TimestampedEntity {
    @OneToOne(optional = false) @JoinColumn(name = "finance_request_id", nullable = false, unique = true)
    private FinanceRequest request;
    private UUID supplierId; private UUID purchaseOrderId; private UUID employeeId; private UUID payrollId;
    private UUID departmentId; private UUID cashBoxId; private UUID recipientEmployeeId; private UUID originalOperationId;
    @Column(length = 255) private String payeeName; @Column(length = 2000) private String purpose;
    @Column(length = 255) private String invoiceReference; @Column(length = 255) private String expenseCategory;
    @Column(length = 2000) private String varianceReason;
    @Column(precision = 19, scale = 2) private BigDecimal expectedAmount;
    @Column(precision = 19, scale = 2) private BigDecimal countedAmount;
    private LocalDate settlementDate;
}
