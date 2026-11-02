package q.qedet.organization.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.common.exception.ConflictException;
import q.qedet.common.model.LocationStatus;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.entity.OrganizationSite;
import q.qedet.organization.entity.OrganizationZone;
import q.qedet.organization.repository.OrganizationSiteRepository;
import q.qedet.organization.repository.OrganizationZoneRepository;

import java.util.*;

/** The single registration provisioning entry point for the canonical site topology. */
@Service
public class OrganizationSiteProvisioningService {
    public static final String DEFAULT_ZONE_CODE = "DEFAULT";
    private final OrganizationSiteRepository sites;
    private final OrganizationZoneRepository zones;

    public OrganizationSiteProvisioningService(OrganizationSiteRepository sites, OrganizationZoneRepository zones) {
        this.sites = sites;
        this.zones = zones;
    }

    @Transactional
    public Map<String, OrganizationSite> provisionRegistrationSites(Organization organization) {
        if (organization == null || organization.getId() == null) throw new ConflictException("Organization must be persisted before site provisioning");
        Map<String, OrganizationSite> result = new HashMap<>();
        for (OrganizationSiteTemplate template : OrganizationSiteTemplate.values()) {
            OrganizationSite site = sites.findByOrganizationIdAndCodeIgnoreCase(organization.getId(), template.code())
                    .orElseGet(() -> {
                        OrganizationSite created = new OrganizationSite(organization, template.code(), template.displayName(), template.type());
                        created.setDefault(template.defaultSite());
                        return sites.save(created);
                    });
            if (!site.getOrganization().getId().equals(organization.getId())) throw new ConflictException("Site ownership does not match organization");
            ensureExactlyOneActiveDefaultZone(site);
            result.put(template.code(), site);
        }
        return result;
    }

    @Transactional
    public OrganizationZone ensureExactlyOneActiveDefaultZone(OrganizationSite site) {
        if (site == null || site.getId() == null) throw new ConflictException("Site must be persisted before zone provisioning");
        List<OrganizationZone> defaults = zones.findAllBySiteIdOrderByNameAsc(site.getId()).stream()
                .filter(z -> z.isActive() && z.isDefault()).toList();
        OrganizationZone selected;
        if (defaults.isEmpty()) {
            selected = zones.findBySiteIdAndCodeIgnoreCase(site.getId(), DEFAULT_ZONE_CODE)
                    .orElseGet(() -> new OrganizationZone(site, DEFAULT_ZONE_CODE, "Default"));
            selected.setActive(true);
            selected.setDefault(true);
            selected = zones.save(selected);
        } else {
            selected = defaults.get(0);
            for (int i = 1; i < defaults.size(); i++) { defaults.get(i).setDefault(false); zones.save(defaults.get(i)); }
        }
        return selected;
    }
}
