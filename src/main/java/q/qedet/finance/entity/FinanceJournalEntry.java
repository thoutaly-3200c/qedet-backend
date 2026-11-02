package q.qedet.finance.entity;
import jakarta.persistence.*; import lombok.*; import q.qedet.common.model.TimestampedEntity; import q.qedet.organization.entity.Organization; import java.time.*;
@Entity @Table(name = "finance_journal_entries", indexes = @Index(name = "idx_finance_journal_org_posted", columnList = "organization_id,posted_at"))
@Getter @Setter @NoArgsConstructor public class FinanceJournalEntry extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @Column(nullable = false, length = 160) private String reference; @Column(length = 2000) private String description; @Column(nullable = false) private boolean posted; private Instant postedAt;
}
