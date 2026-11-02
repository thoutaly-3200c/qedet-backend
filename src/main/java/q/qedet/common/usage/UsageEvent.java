package q.qedet.common.usage;

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
@Table(name = "usage_events")
@Getter
@Setter
@NoArgsConstructor
public class UsageEvent extends TimestampedEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(nullable = false)
    private String moduleName;

    @Column(nullable = false)
    private String actionName;

    @Column(length = 2000)
    private String details;

    public UsageEvent(Organization organization, Employee employee, String moduleName, String actionName, String details) {
        this.organization = organization;
        this.employee = employee;
        this.moduleName = moduleName;
        this.actionName = actionName;
        this.details = details;
    }
}
