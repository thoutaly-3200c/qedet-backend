package q.qedet.procurement.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import q.qedet.procurement.entity.PurchaseRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, UUID> {
    List<PurchaseRequest> findAllByOrganizationId(UUID org);
    Optional<PurchaseRequest> findByIdAndOrganizationId(UUID id, UUID org);
    long countByOrganizationId(UUID org);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PurchaseRequest r where r.id = :id and r.organization.id = :organizationId")
    Optional<PurchaseRequest> findLockedByIdAndOrganizationId(
            @Param("id") UUID id, @Param("organizationId") UUID organizationId);
}
