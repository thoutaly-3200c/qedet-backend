package q.qedet.warehouse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.warehouse.entity.StockMovement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {
    List<StockMovement> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    List<StockMovement> findAllByOrganizationIdAndProductIdOrderByCreatedAtDesc(UUID organizationId, UUID productId);
    Optional<StockMovement> findByIdAndOrganizationId(UUID id, UUID organizationId);
    boolean existsByOrganizationIdAndReference(UUID organizationId, String reference);
}
