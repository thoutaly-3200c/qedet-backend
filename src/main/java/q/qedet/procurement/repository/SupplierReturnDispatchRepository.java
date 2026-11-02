package q.qedet.procurement.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.procurement.entity.SupplierReturnDispatch;
import java.util.*;
public interface SupplierReturnDispatchRepository extends JpaRepository<SupplierReturnDispatch,UUID> {
 boolean existsByOperationIdAndReference(UUID operationId,String reference);
 List<SupplierReturnDispatch> findAllByOperationIdOrderByDispatchedAtAsc(UUID operationId);
}
