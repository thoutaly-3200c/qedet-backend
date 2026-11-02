package q.qedet.procurement.repository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import q.qedet.procurement.entity.SupplierReturnOperation;
import java.util.*;
public interface SupplierReturnOperationRepository extends JpaRepository<SupplierReturnOperation,UUID> {
 Page<SupplierReturnOperation> findAllByOrganizationId(UUID organizationId,Pageable pageable);
 List<SupplierReturnOperation> findAllByWarehouseReturnId(UUID returnId);
 Optional<SupplierReturnOperation> findByIdAndOrganizationId(UUID id,UUID organizationId);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select o from SupplierReturnOperation o where o.id = :id and o.organization.id = :organizationId")
 Optional<SupplierReturnOperation> findForUpdate(UUID id,UUID organizationId);
}
