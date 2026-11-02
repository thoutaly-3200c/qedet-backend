package q.qedet.sales.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.sales.entity.PosSale;
import java.util.Optional;
import java.util.UUID;

public interface PosSaleRepository extends JpaRepository<PosSale, UUID> {
    Optional<PosSale> findByOrganizationIdAndCashierIdAndIdempotencyKey(UUID organizationId, UUID cashierId, String idempotencyKey);
    Optional<PosSale> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
