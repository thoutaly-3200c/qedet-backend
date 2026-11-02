package q.qedet.organization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.common.model.LocationStatus;
import q.qedet.common.model.LocationType;
import q.qedet.organization.entity.OrganizationSite;
import java.util.*;

public interface OrganizationSiteRepository extends JpaRepository<OrganizationSite, UUID> {
    Optional<OrganizationSite> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Optional<OrganizationSite> findByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);
    List<OrganizationSite> findAllByOrganizationIdAndStatusOrderByNameAsc(UUID organizationId, LocationStatus status);
    Optional<OrganizationSite> findByOrganizationIdAndType(UUID organizationId, LocationType type);
    Optional<OrganizationSite> findByOrganizationIdAndTypeAndStatus(UUID organizationId, LocationType type, LocationStatus status);
    List<OrganizationSite> findAllByOrganizationIdOrderByNameAsc(UUID organizationId);
}
