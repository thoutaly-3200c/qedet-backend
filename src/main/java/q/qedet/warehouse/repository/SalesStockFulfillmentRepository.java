package q.qedet.warehouse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.*;
import q.qedet.warehouse.entity.SalesStockFulfillment;

public interface SalesStockFulfillmentRepository extends JpaRepository<SalesStockFulfillment, UUID> {
    List<SalesStockFulfillment> findAllByOrganizationIdAndSalesStockRequestIdOrderByCreatedAtAsc(UUID organizationId, UUID requestId);
    Optional<SalesStockFulfillment> findByIdAndOrganizationId(UUID id, UUID organizationId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from SalesStockFulfillment f where f.id = :id and f.organization.id = :organizationId")
    Optional<SalesStockFulfillment> findForUpdateByIdAndOrganizationId(UUID id, UUID organizationId);
}
