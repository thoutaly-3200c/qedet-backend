package q.qedet.hr.attendance.entity;

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
import q.qedet.hr.attendance.model.AttendanceEventType;
import q.qedet.organization.entity.OrganizationSite;
import q.qedet.organization.entity.Organization;

import java.time.Instant;

@Entity
@Table(name = "hr_attendance_events")
@Getter
@Setter
@NoArgsConstructor
public class AttendanceEvent extends TimestampedEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "attendance_record_id")
    private AttendanceRecord attendanceRecord;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceEventType type;

    @Column(nullable = false)
    private Instant occurredAt;

    @ManyToOne
    @JoinColumn(name = "site_id")
    private OrganizationSite site;

    @Column(length = 1000)
    private String details;

    @Column(length = 32)
    private String source;

    @Column(length = 4000)
    private String metadata;

    public AttendanceEvent(Organization organization, Employee employee, AttendanceRecord record,
                           AttendanceEventType type, Instant occurredAt, String details) {
        this.organization = organization;
        this.employee = employee;
        this.attendanceRecord = record;
        this.type = type;
        this.occurredAt = occurredAt;
        this.details = details;
    }
}

