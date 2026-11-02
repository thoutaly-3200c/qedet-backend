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
import q.qedet.hr.attendance.model.PunchSource;
import q.qedet.hr.attendance.model.PunchType;
import q.qedet.organization.entity.Organization;

import java.time.Instant;

@Entity
@Table(name = "hr_attendance_punches")
@Getter
@Setter
@NoArgsConstructor
public class AttendancePunch extends TimestampedEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PunchType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PunchSource source;

    @Column(nullable = false)
    private Instant punchedAt;

    @Column(length = 500)
    private String note;

    public AttendancePunch(Organization organization, Employee employee, PunchType type,
                           PunchSource source, Instant punchedAt, String note) {
        this.organization = organization;
        this.employee = employee;
        this.type = type;
        this.source = source;
        this.punchedAt = punchedAt;
        this.note = note;
    }
}

