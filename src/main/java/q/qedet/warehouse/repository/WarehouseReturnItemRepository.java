package q.qedet.warehouse.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import q.qedet.warehouse.entity.WarehouseReturnItem;
import java.util.*;
public interface WarehouseReturnItemRepository extends JpaRepository<WarehouseReturnItem, UUID> {
 Optional<WarehouseReturnItem> findByIdAndWarehouseReturnOrganizationId(UUID id, UUID organizationId);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select i from WarehouseReturnItem i where i.id = :id and i.warehouseReturn.organization.id = :organizationId")
 Optional<WarehouseReturnItem> findForUpdateByIdAndOrganizationId(@Param("id") UUID id, @Param("organizationId") UUID organizationId);
}
