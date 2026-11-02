package q.qedet.sales.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.employee.entity.Employee;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.entity.OrganizationSite;

@Entity
@Table(name = "sales_pos_assignments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pos_assignment_cashier", columnNames = {"organization_id", "cashier_id"}),
        @UniqueConstraint(name = "uk_pos_assignment_code", columnNames = {"organization_id", "code"})
})
@Getter @Setter @NoArgsConstructor
public class PosAssignment extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @ManyToOne(optional = false) @JoinColumn(name = "cashier_id", nullable = false)
    private Employee cashier;
    @ManyToOne(optional = false) @JoinColumn(name = "sales_hall_id", nullable = false)
    private OrganizationSite salesHall;
    @Column(nullable = false, length = 40)
    private String code;
    @Column(nullable = false)
    private boolean active = true;
}
