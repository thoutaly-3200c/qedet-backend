package q.qedet.finance.entity;
import jakarta.persistence.*; import lombok.*; import q.qedet.common.model.TimestampedEntity; import java.math.BigDecimal;
@Entity @Table(name = "finance_journal_entry_lines", indexes = @Index(name = "idx_finance_journal_line_entry", columnList = "journal_entry_id"))
@Getter @Setter @NoArgsConstructor public class FinanceJournalEntryLine extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "journal_entry_id", nullable = false) private FinanceJournalEntry journalEntry;
    @ManyToOne(optional = false) @JoinColumn(name = "chart_account_id", nullable = false) private FinanceChartOfAccount chartAccount;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal debit = BigDecimal.ZERO; @Column(nullable = false, precision = 19, scale = 2) private BigDecimal credit = BigDecimal.ZERO;
}
