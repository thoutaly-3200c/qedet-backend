package q.qedet.finance.service;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import q.qedet.common.exception.BadRequestException; import q.qedet.common.exception.ConflictException; import q.qedet.finance.entity.*; import java.math.*; import java.time.Instant; import java.util.*;
@Service public class FinanceAccountingService {
    @Transactional public FinanceJournalEntry post(FinanceJournalEntry entry, Collection<FinanceJournalEntryLine> lines) {
        if (entry.isPosted()) throw new ConflictException("Journal entry is already posted");
        if (lines == null || lines.size() < 2) throw new BadRequestException("A journal entry requires at least two lines");
        BigDecimal debit = lines.stream().map(FinanceJournalEntryLine::getDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credit = lines.stream().map(FinanceJournalEntryLine::getCredit).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (debit.signum() <= 0 || debit.compareTo(credit) != 0) throw new BadRequestException("Journal entry debit and credit totals must balance");
        entry.setPosted(true); entry.setPostedAt(Instant.now()); return entry;
    }
}
