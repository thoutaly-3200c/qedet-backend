package q.qedet.hr.payroll.entity;

import jakarta.persistence.*;
import lombok.*;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.employee.entity.Employee;
import q.qedet.hr.payroll.model.*;
import q.qedet.organization.entity.Organization;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name = "hr_payroll_items") @Getter @Setter @NoArgsConstructor
public class PayrollItem extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @ManyToOne(optional = false) @JoinColumn(name = "employee_id", nullable = false) private Employee employee;
    @ManyToOne(optional = false) @JoinColumn(name = "payroll_period_id", nullable = false) private PayrollPeriod payrollPeriod;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private PayrollItemType type;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private PayrollItemDirection direction;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal amount;
    @Column(nullable = false) private LocalDate effectiveDate;
    @Enumerated(EnumType.STRING) private PayrollItemSourceType sourceType;
    private UUID sourceId;
    @Column(length = 1000) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private PayrollItemStatus status = PayrollItemStatus.PENDING;
    private UUID createdBy;
}
