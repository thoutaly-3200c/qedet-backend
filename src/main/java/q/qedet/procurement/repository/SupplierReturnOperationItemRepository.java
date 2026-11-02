package q.qedet.procurement.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import q.qedet.procurement.entity.SupplierReturnOperationItem;
import java.math.BigDecimal;
import java.util.*;
public interface SupplierReturnOperationItemRepository extends JpaRepository<SupplierReturnOperationItem,UUID> {
 boolean existsByWarehouseReturnItemId(UUID itemId);
 @Query("select coalesce(sum(i.approvedQuantity),0) from SupplierReturnOperationItem i where i.receivingOperationItem.id = :receivingItemId")
 BigDecimal approvedForReceivingItem(@Param("receivingItemId") UUID receivingItemId);
}
