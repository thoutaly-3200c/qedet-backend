package q.qedet.hr.recruitment.entity;

import jakarta.persistence.*;
import lombok.*;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.hr.recruitment.model.JobOpeningStatus;
import q.qedet.hr.recruitment.model.EmploymentType;
import q.qedet.organization.entity.Organization;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity @Table(name = "hr_job_openings") @Getter @Setter @NoArgsConstructor
public class JobOpening extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @Column(nullable = false) private String title;
    @Column(length = 4000) private String description;
    @Column(nullable = false) private UUID departmentId;
    @Column(nullable = false) private UUID positionId;
    @Column(nullable = false) private UUID locationId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private EmploymentType employmentType;
    @ElementCollection
    @CollectionTable(name = "hr_job_requirements", joinColumns = @JoinColumn(name = "job_opening_id"))
    @Column(name = "requirement", length = 1000)
    private List<String> requirements = new ArrayList<>();
    @ElementCollection
    @CollectionTable(name = "hr_job_responsibilities", joinColumns = @JoinColumn(name = "job_opening_id"))
    @Column(name = "responsibility", length = 1000)
    private List<String> responsibilities = new ArrayList<>();
    @Column(nullable = false) private Integer vacancies;
    @Column(nullable = false) private LocalDate applicationDeadline;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private JobOpeningStatus status = JobOpeningStatus.DRAFT;
}
