package q.qedet.finance.entity;

import jakarta.persistence.*;
import lombok.*;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.employee.entity.Employee;
import q.qedet.organization.entity.Organization;

@Entity
@Table(name = "finance_cash_box_employee_assignments",
        uniqueConstraints = @UniqueConstraint(name = "uk_finance_cash_box_employee", columnNames = {"cash_box_id", "employee_id"}),
        indexes = @Index(name = "idx_finance_cash_box_assignment_employee", columnList = "organization_id,employee_id"))
@Getter @Setter @NoArgsConstructor
public class CashBoxEmployeeAssignment extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @ManyToOne(optional = false) @JoinColumn(name = "cash_box_id", nullable = false) private CashBox cashBox;
    @ManyToOne(optional = false) @JoinColumn(name = "employee_id", nullable = false) private Employee employee;
}
