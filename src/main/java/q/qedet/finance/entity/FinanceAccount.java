package q.qedet.finance.entity;
import jakarta.persistence.*; import lombok.*; import q.qedet.common.model.TimestampedEntity; import q.qedet.finance.model.FinanceWorkflowTypes.*; import q.qedet.organization.entity.Organization; import java.util.UUID;
@Entity @Table(name = "finance_accounts", uniqueConstraints = @UniqueConstraint(name = "uk_finance_account_org_name", columnNames = {"organization_id","name"}), indexes = @Index(name = "idx_finance_account_org_type_status", columnList = "organization_id,type,status"))
@Getter @Setter @NoArgsConstructor public class FinanceAccount extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private AccountType type;
    @Column(nullable = false, length = 160) private String name; private UUID locationId; @Column(nullable = false, length = 3) private String currency;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private AccountStatus status = AccountStatus.ACTIVE; private UUID assignedEmployeeId;
}
