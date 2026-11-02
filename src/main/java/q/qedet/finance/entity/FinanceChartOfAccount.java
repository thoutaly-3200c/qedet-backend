package q.qedet.finance.entity;
import jakarta.persistence.*; import lombok.*; import q.qedet.common.model.TimestampedEntity; import q.qedet.organization.entity.Organization;
@Entity @Table(name = "finance_chart_of_accounts", uniqueConstraints = @UniqueConstraint(name = "uk_finance_coa_org_code", columnNames = {"organization_id","code"}))
@Getter @Setter @NoArgsConstructor public class FinanceChartOfAccount extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @Column(nullable = false, length = 32) private String code; @Column(nullable = false, length = 160) private String name; @Column(nullable = false, length = 32) private String accountType; private boolean active = true;
}
