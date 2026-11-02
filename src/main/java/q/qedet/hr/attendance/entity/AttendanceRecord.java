package q.qedet.hr.attendance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.employee.entity.Employee;
import q.qedet.hr.attendance.model.AttendanceStatus;
import q.qedet.organization.entity.OrganizationSite;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.policy.entity.WorkSchedule;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "hr_attendance_records",
        uniqueConstraints = @UniqueConstraint(name = "uk_hr_attendance_employee_date", columnNames = {"employee_id", "work_date"}),
        indexes = {
                @Index(name = "idx_hr_attendance_org_date", columnList = "organization_id,work_date"),
                @Index(name = "idx_hr_attendance_org_status", columnList = "organization_id,status")
        })
@Getter
@Setter
@NoArgsConstructor
public class AttendanceRecord extends TimestampedEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "schedule_id")
    private WorkSchedule schedule;

    @ManyToOne
    @JoinColumn(name = "site_id")
    private OrganizationSite site;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status = AttendanceStatus.INCOMPLETE;

    private Instant checkInAt;
    private Instant checkOutAt;
    private Integer scheduledMinutes = 0;
    private Integer workedMinutes = 0;
    private Integer lateMinutes = 0;
    private Integer earlyLeaveMinutes = 0;
    private Integer overtimeMinutes = 0;
    private boolean authorizedLeave;

    public AttendanceRecord(Organization organization, Employee employee, LocalDate workDate) {
        this.organization = organization;
        this.employee = employee;
        this.workDate = workDate;
    }
}
