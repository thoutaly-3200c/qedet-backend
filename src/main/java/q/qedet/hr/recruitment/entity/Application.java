package q.qedet.hr.recruitment.entity;

import jakarta.persistence.*;
import lombok.*;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.hr.recruitment.model.ApplicationStatus;
import q.qedet.organization.entity.Organization;

@Entity @Table(name = "hr_job_applications",
        uniqueConstraints = @UniqueConstraint(name = "uk_hr_application_candidate_job", columnNames = {"candidate_id", "job_opening_id"}))
@Getter @Setter @NoArgsConstructor
public class Application extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @ManyToOne(optional = false) @JoinColumn(name = "candidate_id", nullable = false) private Candidate candidate;
    @ManyToOne(optional = false) @JoinColumn(name = "job_opening_id", nullable = false) private JobOpening jobOpening;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ApplicationStatus status = ApplicationStatus.APPLIED;
    @Column(length = 2000) private String resumeKey;
    @Column(length = 2000) private String notes;
}
