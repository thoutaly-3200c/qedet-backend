package q.qedet.sales.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import q.qedet.sales.entity.SalesStockRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SalesStockRequestRepository extends JpaRepository<SalesStockRequest, UUID> {
    List<SalesStockRequest> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    Optional<SalesStockRequest> findByIdAndOrganizationId(UUID id, UUID organizationId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from SalesStockRequest r where r.id = :id and r.organization.id = :organizationId")
    Optional<SalesStockRequest> findForUpdateByIdAndOrganizationId(UUID id, UUID organizationId);
}
