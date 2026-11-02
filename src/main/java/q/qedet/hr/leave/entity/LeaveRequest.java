package q.qedet.hr.leave.entity;

import jakarta.persistence.*;
import lombok.*;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.employee.entity.Employee;
import q.qedet.hr.leave.model.LeaveRequestStatus;
import q.qedet.organization.entity.Organization;
import java.time.LocalDate;

@Entity @Table(name = "hr_leave_requests") @Getter @Setter @NoArgsConstructor
public class LeaveRequest extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @ManyToOne(optional = false) @JoinColumn(name = "employee_id", nullable = false) private Employee employee;
    @ManyToOne(optional = false) @JoinColumn(name = "leave_type_id", nullable = false) private LeaveType leaveType;
    @Column(nullable = false) private LocalDate startDate;
    @Column(nullable = false) private LocalDate endDate;
    @Column(length = 2000) private String reason;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private LeaveRequestStatus status = LeaveRequestStatus.REQUESTED;
}
