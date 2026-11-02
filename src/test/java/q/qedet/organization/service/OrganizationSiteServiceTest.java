package q.qedet.organization.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.ConflictException;
import q.qedet.common.exception.ForbiddenException;
import q.qedet.common.model.LocationStatus;
import q.qedet.common.model.LocationType;
import q.qedet.common.model.Permission;
import q.qedet.organization.dto.OrganizationSiteRequest;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.entity.OrganizationSite;
import q.qedet.organization.repository.*;
import q.qedet.employee.repository.EmployeeRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationSiteServiceTest {
    @Mock OrganizationSiteRepository sites;
    @Mock OrganizationRepository organizations;
    @Mock OrganizationSiteProvisioningService provisioning;
    @Mock ActorContext actor;
    @Mock PositionRepository positions;
    @Mock OrganizationZoneRepository zones;
    @Mock EmployeeRepository employees;
    Organization organization;

    @BeforeEach void setUp() {
        organization = new Organization(); organization.setId(UUID.randomUUID());
        when(actor.organizationId()).thenReturn(organization.getId());
    }

    @Test void createsSiteAndProvisionsDefaultZone() {
        when(organizations.findByIdForUpdate(organization.getId())).thenReturn(Optional.of(organization));
        when(sites.save(any(OrganizationSite.class))).thenAnswer(invocation -> {
            OrganizationSite site = invocation.getArgument(0); site.setId(UUID.randomUUID()); return site;
        });
        var result = service().create(organization.getId(), request("MAIN", "Main Store", true));
        assertThat(result.type()).isEqualTo(LocationType.STORE);
        assertThat(result.isDefault()).isTrue();
        verify(provisioning).ensureExactlyOneActiveDefaultZone(argThat(site -> site.getId().equals(result.id())));
        verify(actor).requirePermission(Permission.LOCATION_MANAGE);
    }

    @Test void rejectsDuplicateCodeWithinOrganization() {
        OrganizationSite existing = new OrganizationSite(); existing.setId(UUID.randomUUID());
        when(sites.findByOrganizationIdAndCodeIgnoreCase(organization.getId(), "MAIN"))
                .thenReturn(Optional.of(existing));
        assertThatThrownBy(() -> service().create(organization.getId(), request("MAIN", "Another", false)))
                .isInstanceOf(ConflictException.class);
        verify(organizations, never()).findByIdForUpdate(any());
    }

    @Test void excludesOtherOrganizations() {
        assertThatThrownBy(() -> service().list(UUID.randomUUID())).isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(sites);
    }

    @Test void deactivationClearsDefaultFlag() {
        OrganizationSite site = new OrganizationSite(organization, "MAIN", "Main Store", LocationType.STORE);
        site.setId(UUID.randomUUID()); site.setDefault(true);
        when(sites.findByIdAndOrganizationId(site.getId(), organization.getId())).thenReturn(Optional.of(site));
        when(sites.save(site)).thenReturn(site);
        var result = service().deactivate(organization.getId(), site.getId());
        assertThat(result.status()).isEqualTo(LocationStatus.INACTIVE);
        assertThat(result.isDefault()).isFalse();
    }

    private OrganizationSiteRequest request(String code, String name, boolean selected) {
        return new OrganizationSiteRequest(code, name, LocationType.STORE, "Retail site", "123 Main Street",
                null, "Cairo", "Cairo", "11511", "Egypt", "+20 100 000 0000", "main@example.com", selected);
    }
    private OrganizationSiteService service() {
        return new OrganizationSiteService(sites, organizations, provisioning, actor, positions, zones, employees);
    }
}
