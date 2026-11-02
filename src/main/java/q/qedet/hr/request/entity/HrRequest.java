package q.qedet.hr.request.entity;

import jakarta.persistence.*;
import lombok.*;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.employee.entity.Employee;
import q.qedet.hr.request.model.*;
import q.qedet.organization.entity.Organization;

@Entity @Table(name = "hr_requests") @Getter @Setter @NoArgsConstructor
public class HrRequest extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @ManyToOne(optional = false) @JoinColumn(name = "employee_id", nullable = false) private Employee employee;
    @ManyToOne(optional = false) @JoinColumn(name = "requested_by_employee_id", nullable = false) private Employee requestedBy;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private HrRequestType type;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private HrRequestStatus status = HrRequestStatus.DRAFT;
    @Column(length = 4000) private String details;
    @Column(length = 2000) private String decisionNote;
}
