package q.qedet.sales.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import q.qedet.sales.entity.SalesPackage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SalesPackageRepository extends JpaRepository<SalesPackage, UUID> {
    List<SalesPackage> findAllByOrganizationIdAndSalesHallIdOrderByCreatedAtDesc(UUID organizationId, UUID salesHallId);
    Optional<SalesPackage> findByIdAndOrganizationId(UUID id, UUID organizationId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SalesPackage> findLockedByIdAndOrganizationId(UUID id, UUID organizationId);
}
