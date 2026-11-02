package q.qedet.organization.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.entity.OrganizationSite;
import q.qedet.organization.entity.OrganizationZone;
import q.qedet.organization.repository.OrganizationSiteRepository;
import q.qedet.organization.repository.OrganizationZoneRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationSiteProvisioningServiceTest {
    @Mock OrganizationSiteRepository sites;
    @Mock OrganizationZoneRepository zones;

    @Test void registrationCreatesCanonicalSitesAndDefaultZones() {
        Organization organization = new Organization(); organization.setId(UUID.randomUUID());
        when(sites.save(any(OrganizationSite.class))).thenAnswer(invocation -> {
            OrganizationSite site = invocation.getArgument(0); site.setId(UUID.randomUUID()); return site;
        });
        when(zones.save(any(OrganizationZone.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service().provisionRegistrationSites(organization);

        assertThat(result).hasSize(OrganizationSiteTemplate.values().length);
        assertThat(result.get("WAREHOUSE").getType().name()).isEqualTo("WAREHOUSE");
        verify(sites, times(OrganizationSiteTemplate.values().length)).save(any(OrganizationSite.class));
        verify(zones, times(OrganizationSiteTemplate.values().length)).save(argThat(zone ->
                zone.isActive() && zone.isDefault() && "DEFAULT".equals(zone.getCode())));
    }

    @Test void existingActiveDefaultZoneIsReused() {
        Organization organization = new Organization(); organization.setId(UUID.randomUUID());
        OrganizationSite site = new OrganizationSite(organization, "WAREHOUSE", "Warehouse", q.qedet.common.model.LocationType.WAREHOUSE);
        site.setId(UUID.randomUUID());
        OrganizationZone existing = new OrganizationZone(site, "DEFAULT", "Default");
        existing.setId(UUID.randomUUID()); existing.setActive(true); existing.setDefault(true);
        when(zones.findAllBySiteIdOrderByNameAsc(site.getId())).thenReturn(List.of(existing));

        assertThat(service().ensureExactlyOneActiveDefaultZone(site)).isSameAs(existing);
        verify(zones, never()).save(any());
    }

    private OrganizationSiteProvisioningService service() { return new OrganizationSiteProvisioningService(sites, zones); }
}
