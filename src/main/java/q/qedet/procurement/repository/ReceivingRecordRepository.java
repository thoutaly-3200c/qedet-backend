package q.qedet.procurement.repository;
import org.springframework.data.jpa.repository.JpaRepository; import q.qedet.procurement.entity.ReceivingRecord; import java.util.*;
public interface ReceivingRecordRepository extends JpaRepository<ReceivingRecord,UUID> { Optional<ReceivingRecord> findByPurchaseOrderIdAndOrganizationId(UUID po,UUID org); long countByOrganizationId(UUID org); }
