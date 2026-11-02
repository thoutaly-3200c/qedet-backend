package q.qedet.finance.repository;
import org.springframework.data.jpa.repository.JpaRepository; import q.qedet.finance.entity.FinanceOperationAttachment; import java.util.*;
public interface FinanceOperationAttachmentRepository extends JpaRepository<FinanceOperationAttachment, UUID> { List<FinanceOperationAttachment> findAllByOperationIdOrderByCreatedAtAsc(UUID operationId); }
