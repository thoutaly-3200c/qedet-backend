package q.qedet.finance.repository;
import org.springframework.data.jpa.repository.JpaRepository; import q.qedet.finance.entity.FinanceRequestAttachment; import java.util.*;
public interface FinanceRequestAttachmentRepository extends JpaRepository<FinanceRequestAttachment, UUID> { List<FinanceRequestAttachment> findAllByRequestIdOrderByCreatedAtAsc(UUID requestId); }
