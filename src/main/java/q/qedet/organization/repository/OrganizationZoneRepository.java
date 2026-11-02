package q.qedet.organization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.organization.entity.OrganizationZone;
import java.util.*;

public interface OrganizationZoneRepository extends JpaRepository<OrganizationZone, UUID> {
    Optional<OrganizationZone> findByIdAndSiteId(UUID id, UUID siteId);
    Optional<OrganizationZone> findByIdAndSiteOrganizationId(UUID id, UUID organizationId);
    Optional<OrganizationZone> findByIdAndSiteOrganizationIdAndActiveTrue(UUID id, UUID organizationId);
    Optional<OrganizationZone> findBySiteIdAndCodeIgnoreCase(UUID siteId, String code);
    Optional<OrganizationZone> findBySiteIdAndActiveTrueAndIsDefaultTrue(UUID siteId);
    List<OrganizationZone> findAllBySiteIdAndActiveTrueOrderByNameAsc(UUID siteId);
    List<OrganizationZone> findAllBySiteIdOrderByNameAsc(UUID siteId);
}
