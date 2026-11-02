package q.qedet.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import q.qedet.common.model.TimestampedEntity;
import q.qedet.employee.entity.Employee;
import q.qedet.organization.entity.Organization;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog extends TimestampedEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne
    @JoinColumn(name = "actor_employee_id")
    private Employee actorEmployee;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String targetType;

    @Column(nullable = false)
    private String targetId;

    @Column(length = 4000)
    private String previousValue;

    @Column(length = 4000)
    private String newValue;

    public AuditLog(Organization organization, Employee actorEmployee, String action, String targetType, String targetId, String previousValue, String newValue) {
        this.organization = organization;
        this.actorEmployee = actorEmployee;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.previousValue = previousValue;
        this.newValue = newValue;
    }
}
