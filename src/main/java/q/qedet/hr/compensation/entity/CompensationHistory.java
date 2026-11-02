package q.qedet.hr.compensation.entity;

import jakarta.persistence.*;
import lombok.*;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.employee.entity.Employee;
import q.qedet.organization.entity.Organization;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity @Table(name = "hr_compensation_history") @Getter @Setter @NoArgsConstructor
public class CompensationHistory extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @ManyToOne(optional = false) @JoinColumn(name = "employee_id", nullable = false) private Employee employee;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal baseSalary;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal recurringAllowance;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal overtimeRate;
    @Column(nullable = false) private LocalDate effectiveFrom;
    @Column(length = 1000) private String reason;
}
