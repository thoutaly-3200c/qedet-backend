package q.qedet.finance.repository;
import org.springframework.data.jpa.repository.JpaRepository; import q.qedet.finance.entity.FinanceRequestEvent; import java.util.*;
public interface FinanceRequestEventRepository extends JpaRepository<FinanceRequestEvent, UUID> { List<FinanceRequestEvent> findAllByRequestIdOrderByOccurredAtAsc(UUID requestId); }
