package q.qedet.hr.onboarding.entity;

import jakarta.persistence.*;
import lombok.*;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.employee.entity.Employee;
import q.qedet.hr.onboarding.model.OnboardingTaskStatus;
import q.qedet.organization.entity.Organization;
import java.time.Instant;

@Entity @Table(name = "hr_onboarding_tasks") @Getter @Setter @NoArgsConstructor
public class OnboardingTask extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @ManyToOne(optional = false) @JoinColumn(name = "onboarding_id", nullable = false) private Onboarding onboarding;
    @ManyToOne @JoinColumn(name = "assignee_employee_id") private Employee assignee;
    @Column(nullable = false) private String title;
    @Column(length = 2000) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private OnboardingTaskStatus status = OnboardingTaskStatus.PENDING;
    private Instant completedAt;
}
