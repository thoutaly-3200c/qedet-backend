package q.qedet.procurement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.procurement.entity.DeliveryLocation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryLocationRepository extends JpaRepository<DeliveryLocation, UUID> {
    List<DeliveryLocation> findAllByOrganizationIdOrderByNameAsc(UUID organizationId);
    Optional<DeliveryLocation> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
