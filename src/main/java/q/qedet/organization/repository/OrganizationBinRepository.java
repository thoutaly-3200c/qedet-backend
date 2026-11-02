package q.qedet.organization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.organization.entity.OrganizationBin;
import java.util.*;

public interface OrganizationBinRepository extends JpaRepository<OrganizationBin, UUID> {
    Optional<OrganizationBin> findByIdAndZoneId(UUID id, UUID zoneId);
    Optional<OrganizationBin> findByIdAndZoneSiteOrganizationId(UUID id, UUID organizationId);
    Optional<OrganizationBin> findByZoneIdAndCodeIgnoreCase(UUID zoneId, String code);
    List<OrganizationBin> findAllByZoneIdOrderByNameAsc(UUID zoneId);
}
