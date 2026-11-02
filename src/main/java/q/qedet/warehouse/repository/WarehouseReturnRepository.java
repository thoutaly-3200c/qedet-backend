package q.qedet.warehouse.repository;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.warehouse.entity.WarehouseReturn;
import java.util.*;

public interface WarehouseReturnRepository extends JpaRepository<WarehouseReturn, UUID> {
    Optional<WarehouseReturn> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Page<WarehouseReturn> findAllByOrganizationIdAndTypeAndStatus(UUID organizationId, WarehouseReturn.Type type, WarehouseReturn.Status status, Pageable pageable);
    Page<WarehouseReturn> findAllByOrganizationIdAndType(UUID organizationId, WarehouseReturn.Type type, Pageable pageable);
    Page<WarehouseReturn> findAllByOrganizationIdAndStatus(UUID organizationId, WarehouseReturn.Status status, Pageable pageable);
    Page<WarehouseReturn> findAllByOrganizationId(UUID organizationId, Pageable pageable);
    long countByOrganizationIdAndStatus(UUID organizationId, WarehouseReturn.Status status);
    long countByOrganizationIdAndType(UUID organizationId, WarehouseReturn.Type type);
}
