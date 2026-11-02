package q.qedet.procurement.repository;
import org.springframework.data.jpa.repository.JpaRepository; import q.qedet.procurement.entity.Supplier; import java.util.*;
public interface SupplierRepository extends JpaRepository<Supplier,UUID> {
 List<Supplier> findAllByOrganizationId(UUID org); Optional<Supplier> findByIdAndOrganizationId(UUID id,UUID org); long countByOrganizationIdAndStatus(UUID org,Supplier.SupplierStatus status);
}
