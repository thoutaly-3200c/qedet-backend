package q.qedet.hr.onboarding.entity;

import jakarta.persistence.*;
import lombok.*;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.employee.entity.Employee;
import q.qedet.hr.onboarding.model.OnboardingStatus;
import q.qedet.organization.entity.Organization;
import java.time.Instant;

@Entity @Table(name = "hr_onboardings") @Getter @Setter @NoArgsConstructor
public class Onboarding extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @OneToOne(optional = false) @JoinColumn(name = "employee_id", nullable = false, unique = true) private Employee employee;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private OnboardingStatus status = OnboardingStatus.NOT_STARTED;
    private Instant startedAt;
    private Instant completedAt;
    @Column(length = 2000) private String notes;
}
