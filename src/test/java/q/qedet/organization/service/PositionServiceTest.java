package q.qedet.organization.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.ConflictException;
import q.qedet.common.exception.NotFoundException;
import q.qedet.common.model.LocationType;
import q.qedet.organization.dto.PositionRequest;
import q.qedet.organization.dto.PositionResponse;
import q.qedet.organization.entity.Department;
import q.qedet.organization.entity.OrganizationSite;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.entity.Position;
import q.qedet.organization.repository.DepartmentRepository;
import q.qedet.organization.repository.OrganizationSiteRepository;
import q.qedet.organization.repository.OrganizationRepository;
import q.qedet.organization.repository.PositionRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PositionServiceTest {
    @Mock private PositionRepository positions;
    @Mock private OrganizationRepository organizations;
    @Mock private DepartmentRepository departments;
    @Mock private OrganizationSiteRepository locations;
    @Mock private ActorContext actor;
    @InjectMocks private PositionService service;

    private UUID organizationId;
    private Organization organization;
    private Department department;
    private OrganizationSite location;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        organization = new Organization();
        organization.setId(organizationId);
        department = new Department();
        department.setId(UUID.randomUUID());
        department.setOrganization(organization);
        department.setName("Sales");
        location = new OrganizationSite();
        location.setId(UUID.randomUUID());
        location.setOrganization(organization);
        location.setName("Main Store");
        location.setCode("MAIN");
        location.setType(LocationType.STORE);
        when(actor.organizationId()).thenReturn(organizationId);
    }

    @Test
    void createsPositionWithOrganizationOwnedDepartmentAndDefaultLocation() {
        PositionRequest request = request("SALES_ASSOCIATE", department.getId(), location.getId());
        when(positions.existsByOrganizationIdAndCodeIgnoreCase(organizationId, "SALES_ASSOCIATE")).thenReturn(false);
        when(organizations.findById(organizationId)).thenReturn(Optional.of(organization));
        when(departments.findByIdAndOrganizationId(department.getId(), organizationId)).thenReturn(Optional.of(department));
        when(locations.findByIdAndOrganizationId(location.getId(), organizationId)).thenReturn(Optional.of(location));
        when(positions.save(any(Position.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PositionResponse response = service.create(organizationId, request);

        ArgumentCaptor<Position> captor = ArgumentCaptor.forClass(Position.class);
        verify(positions).save(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("SALES_ASSOCIATE");
        assertThat(captor.getValue().getDepartment()).isEqualTo(department);
        assertThat(captor.getValue().getDefaultSite()).isEqualTo(location);
        assertThat(response.department().name()).isEqualTo("Sales");
    }

    @Test
    void rejectsDuplicateCodesWithinOrganization() {
        when(positions.existsByOrganizationIdAndCodeIgnoreCase(organizationId, "SALES_ASSOCIATE")).thenReturn(true);

        assertThatThrownBy(() -> service.create(organizationId, request("sales_associate", department.getId(), null)))
                .isInstanceOf(ConflictException.class);
        verifyNoInteractions(organizations, departments, locations);
    }

    @Test
    void rejectsDepartmentFromAnotherOrganization() {
        when(positions.existsByOrganizationIdAndCodeIgnoreCase(organizationId, "SALES_ASSOCIATE")).thenReturn(false);
        when(organizations.findById(organizationId)).thenReturn(Optional.of(organization));
        when(departments.findByIdAndOrganizationId(department.getId(), organizationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(organizationId, request("SALES_ASSOCIATE", department.getId(), null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void filtersPositionsByDefaultLocation() {
        Position matching = position("SALES_ASSOCIATE", location);
        Position other = position("STORE_MANAGER", null);
        when(positions.findAllByOrganizationIdOrderByNameAsc(organizationId)).thenReturn(List.of(matching, other));

        List<PositionResponse> response = service.list(organizationId, null, location.getId(), true, null);

        assertThat(response).extracting(PositionResponse::code).containsExactly("SALES_ASSOCIATE");
    }

    private PositionRequest request(String code, UUID departmentId, UUID locationId) {
        return new PositionRequest(code, "Sales Associate", departmentId, locationId, "Retail sales", true);
    }

    private Position position(String code, OrganizationSite defaultLocation) {
        Position position = new Position();
        position.setId(UUID.randomUUID());
        position.setOrganization(organization);
        position.setDepartment(department);
        position.setDefaultSite(defaultLocation);
        position.setCode(code);
        position.setName(code);
        position.setActive(true);
        return position;
    }
}

