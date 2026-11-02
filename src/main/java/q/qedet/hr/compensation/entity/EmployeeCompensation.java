package q.qedet.hr.compensation.entity;

import jakarta.persistence.*;
import lombok.*;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.employee.entity.Employee;
import q.qedet.organization.entity.Organization;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity @Table(name = "hr_employee_compensation") @Getter @Setter @NoArgsConstructor
public class EmployeeCompensation extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @OneToOne(optional = false) @JoinColumn(name = "employee_id", nullable = false, unique = true) private Employee employee;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal baseSalary = BigDecimal.ZERO;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal recurringAllowance = BigDecimal.ZERO;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal overtimeRate = BigDecimal.ZERO;
    @Column(nullable = false) private LocalDate effectiveFrom;
}
