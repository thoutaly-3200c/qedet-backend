package q.qedet.finance.entity;

import jakarta.persistence.*;
import lombok.*;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.employee.entity.Employee;
import q.qedet.finance.model.CashBoxStatus;
import q.qedet.finance.model.CashBoxType;
import q.qedet.organization.entity.OrganizationSite;
import q.qedet.organization.entity.Organization;

@Entity
@Table(name = "finance_cash_boxes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_finance_cash_box_org_name", columnNames = {"organization_id", "name"}),
        @UniqueConstraint(name = "uk_finance_cash_box_org_provisioning", columnNames = {"organization_id", "provisioning_key"}),
        @UniqueConstraint(name = "uk_finance_cash_box_org_owner", columnNames = {"organization_id", "owner_employee_id"})
},
        indexes = @Index(name = "idx_finance_cash_box_org_status", columnList = "organization_id,status"))
@Getter @Setter @NoArgsConstructor
public class CashBox extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @ManyToOne @JoinColumn(name = "site_id") private OrganizationSite site;
    @ManyToOne @JoinColumn(name = "owner_employee_id") private Employee ownerEmployee;
    @Column(nullable = false, length = 160) private String name;
    @Column(nullable = false, length = 3) private String currency;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private CashBoxStatus status = CashBoxStatus.ACTIVE;
    @Enumerated(EnumType.STRING) @Column(name = "cash_box_type", nullable = false, length = 16) private CashBoxType type;
    @Column(name = "provisioning_key", nullable = false, length = 48) private String provisioningKey;
}
