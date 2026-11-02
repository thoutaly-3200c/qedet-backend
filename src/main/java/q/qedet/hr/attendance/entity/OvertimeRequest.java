package q.qedet.hr.attendance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.employee.entity.Employee;
import q.qedet.hr.attendance.model.OvertimeStatus;
import q.qedet.organization.entity.Organization;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hr_attendance_overtime_requests",
        indexes = @Index(name = "idx_hr_overtime_org_status", columnList = "organization_id,status"))
@Getter
@Setter
@NoArgsConstructor
public class OvertimeRequest extends TimestampedEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(optional = false)
    @JoinColumn(name = "attendance_id", nullable = false)
    private AttendanceRecord attendance;

    @Column(nullable = false)
    private Integer minutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OvertimeStatus status = OvertimeStatus.PENDING;

    private String reason;
    private UUID reviewedBy;
    private Instant reviewedAt;
    private String reviewReason;
}


