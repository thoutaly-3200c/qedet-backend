package q.qedet.hr.attendance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.organization.entity.Department;
import q.qedet.organization.entity.OrganizationSite;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.policy.entity.WorkSchedule;

@Entity
@Table(name = "hr_attendance_qrs", uniqueConstraints = @UniqueConstraint(
        name = "uk_hr_attendance_qr_department", columnNames = {"organization_id", "department_id"}))
@Getter
@Setter
@NoArgsConstructor
public class AttendanceQr extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @ManyToOne(optional = false) @JoinColumn(name = "department_id", nullable = false)
    private Department department;
    @ManyToOne(optional = false) @JoinColumn(name = "schedule_id", nullable = false)
    private WorkSchedule schedule;
    @ManyToOne(optional = false) @JoinColumn(name = "site_id", nullable = false)
    private OrganizationSite site;
    @Column(nullable = false)
    private boolean active = true;
}
