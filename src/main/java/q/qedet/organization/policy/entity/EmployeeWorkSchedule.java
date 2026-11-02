package q.qedet.organization.policy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.employee.entity.Employee;
import q.qedet.organization.policy.model.AssignmentStatus;
import q.qedet.organization.entity.Organization;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "hr_employee_work_schedules",
        indexes = @Index(name = "idx_hr_employee_schedule_effective",
                columnList = "organization_id,employee_id,effective_from,effective_to"))
@Getter
@Setter
@NoArgsConstructor
public class EmployeeWorkSchedule extends TimestampedEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(optional = false)
    @JoinColumn(name = "work_schedule_id", nullable = false)
    private WorkSchedule workSchedule;

    @Column(nullable = false)
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignmentStatus status = AssignmentStatus.ACTIVE;

    @Column(nullable = false)
    private UUID assignedBy;

    @Column(nullable = false)
    private Instant assignedAt;
}
