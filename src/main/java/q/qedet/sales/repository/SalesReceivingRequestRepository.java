package q.qedet.sales.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import q.qedet.sales.entity.SalesReceivingRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SalesReceivingRequestRepository extends JpaRepository<SalesReceivingRequest, UUID> {
    boolean existsByFulfillmentId(UUID fulfillmentId);
    Optional<SalesReceivingRequest> findByFulfillmentId(UUID fulfillmentId);
    Optional<SalesReceivingRequest> findByIdAndOrganizationId(UUID id, UUID organizationId);
    List<SalesReceivingRequest> findAllByOrganizationIdAndStatusOrderByCreatedAtDesc(UUID organizationId, String status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from SalesReceivingRequest r where r.id = :id and r.organization.id = :organizationId")
    Optional<SalesReceivingRequest> findForUpdate(UUID id, UUID organizationId);
}
