package q.qedet.employee.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.organization.entity.Department;

import java.time.Instant;

@Entity
@Table(name = "employee_departments", uniqueConstraints = @UniqueConstraint(
        name = "uk_employee_department_assignment", columnNames = {"employee_id", "department_id", "ended_at"}))
@Getter
@Setter
@NoArgsConstructor
public class EmployeeDepartment extends TimestampedEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(nullable = false)
    private boolean primaryAssignment;

    @Column(nullable = false, updatable = false)
    private Instant assignedAt = Instant.now();

    private Instant endedAt;
}
