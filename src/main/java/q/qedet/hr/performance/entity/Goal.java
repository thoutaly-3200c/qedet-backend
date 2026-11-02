package q.qedet.hr.performance.entity;

import jakarta.persistence.*;
import lombok.*;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.employee.entity.Employee;
import q.qedet.hr.performance.model.GoalStatus;
import q.qedet.organization.entity.Organization;
import java.time.*;

@Entity @Table(name = "hr_performance_goals") @Getter @Setter @NoArgsConstructor
public class Goal extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @ManyToOne(optional = false) @JoinColumn(name = "employee_id", nullable = false) private Employee employee;
    @Column(nullable = false) private String title;
    @Column(length = 2000) private String description;
    private LocalDate dueDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private GoalStatus status = GoalStatus.ACTIVE;
    private Instant completedAt;
}
