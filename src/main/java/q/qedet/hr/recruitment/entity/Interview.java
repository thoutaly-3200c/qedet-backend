package q.qedet.hr.recruitment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.employee.entity.Employee;
import q.qedet.hr.recruitment.model.InterviewResult;
import q.qedet.hr.recruitment.model.InterviewStatus;
import q.qedet.organization.entity.Organization;

import java.time.Instant;

@Entity
@Table(name = "hr_interviews")
@Getter
@Setter
@NoArgsConstructor
public class Interview extends TimestampedEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(optional = false)
    @JoinColumn(name = "interviewer_id", nullable = false)
    private Employee interviewer;

    @Column(nullable = false)
    private Instant scheduledAt;

    @Column(nullable = false)
    private Integer durationMinutes = 60;

    @Column(length = 500)
    private String location;

    @Column(length = 2000)
    private String meetingLink;

    @Column(length = 4000)
    private String notes;

    @Column(length = 4000)
    private String feedback;

    @Enumerated(EnumType.STRING)
    private InterviewResult result;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewStatus status = InterviewStatus.SCHEDULED;
}
