package q.qedet.organization.policy.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.organization.policy.model.WorkScheduleStatus;
import q.qedet.organization.entity.Organization;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hr_work_schedules",
        uniqueConstraints = @UniqueConstraint(name = "uk_hr_schedule_org_name",
                columnNames = {"organization_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
public class WorkSchedule extends TimestampedEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private String timezone = "UTC";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkScheduleStatus status = WorkScheduleStatus.DRAFT;

    @Column(nullable = false)
    private Integer gracePeriodMinutes = 0;

    @Column(nullable = false)
    private boolean organizationDefault;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hr_work_schedule_days",
            joinColumns = @JoinColumn(name = "schedule_id"))
    private List<WorkdayDefinition> workdays = new ArrayList<>();
}
