package q.qedet.organization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.organization.entity.Position;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PositionRepository extends JpaRepository<Position, UUID> {
    List<Position> findAllByOrganizationIdOrderByNameAsc(UUID organizationId);
    Optional<Position> findByIdAndOrganizationId(UUID id, UUID organizationId);
    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);
    boolean existsByDefaultSiteId(UUID siteId);
}
