package q.qedet.hr.compensation.entity;

import jakarta.persistence.*;
import lombok.*;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.employee.entity.Employee;
import q.qedet.organization.entity.Organization;
import java.time.LocalDate;

@Entity @Table(name = "hr_employee_benefits") @Getter @Setter @NoArgsConstructor
public class EmployeeBenefit extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @ManyToOne(optional = false) @JoinColumn(name = "employee_id", nullable = false) private Employee employee;
    @Column(nullable = false) private String benefitCode;
    @Column(length = 1000) private String description;
    @Column(nullable = false) private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    @Column(nullable = false) private boolean active = true;
}
