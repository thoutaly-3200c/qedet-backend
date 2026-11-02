package q.qedet.organization.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import q.qedet.auth.entity.UserAccount;
import q.qedet.auth.repository.UserAccountRepository;
import q.qedet.auth.security.ActorContext;
import q.qedet.auth.security.TokenService;
import q.qedet.common.usage.UsageTracker;
import q.qedet.employee.entity.Employee;
import q.qedet.employee.entity.Role;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.employee.repository.RoleRepository;
import q.qedet.employee.service.DefaultRolePermissionProvisioningService;
import q.qedet.finance.service.CashBoxProvisioningService;
import q.qedet.organization.dto.RegistrationRequest;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.repository.DepartmentRepository;
import q.qedet.organization.repository.OrganizationRepository;
import q.qedet.organization.repository.PositionRepository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceWarehouseProvisioningTest {
    @Mock private OrganizationRepository organizations;
    @Mock private ActorContext actor;
    @Mock private EmployeeRepository employees;
    @Mock private DepartmentRepository departments;
    @Mock private OrganizationSiteProvisioningService sites;
    @Mock private TokenService tokens;
    @Mock private UsageTracker usage;
    @Mock private UserAccountRepository users;
    @Mock private RoleRepository roles;
    @Mock private PositionRepository positions;
    @Mock private CashBoxProvisioningService cashBoxes;
    @Mock private DefaultRolePermissionProvisioningService permissions;

    @Test
    void registrationProvisionsTheWarehouseBeforeCreatingTheOwner() {
        Organization organization = new Organization();
        organization.setId(UUID.randomUUID());
        when(organizations.findByNameIgnoreCase("New Org")).thenReturn(Optional.empty());
        when(organizations.save(any(Organization.class))).thenReturn(organization);
        when(employees.countByOrganizationId(organization.getId())).thenReturn(0L);
        when(employees.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee employee = invocation.getArgument(0);
            employee.setId(UUID.randomUUID());
            return employee;
        });
        when(departments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(sites.provisionRegistrationSites(organization)).thenReturn(Map.of());
        when(roles.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokens.createSession(any(), any(), eq(false))).thenReturn(new TokenService.TokenPair("access", "refresh"));

        service().create(user(), request());

        var order = inOrder(sites, employees);
        order.verify(sites).provisionRegistrationSites(organization);
        order.verify(employees).save(any(Employee.class));
    }

    @Test
    void provisioningFailureStopsRegistrationBeforeAnyOwnerIsCreated() {
        Organization organization = new Organization();
        organization.setId(UUID.randomUUID());
        when(organizations.findByNameIgnoreCase("New Org")).thenReturn(Optional.empty());
        when(organizations.save(any(Organization.class))).thenReturn(organization);
        doThrow(new IllegalStateException("warehouse provisioning failed"))
                .when(sites).provisionRegistrationSites(organization);

        assertThatThrownBy(() -> service().create(user(), request()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("warehouse provisioning failed");

        verify(employees, never()).save(any());
        verifyNoInteractions(departments, positions, roles, permissions, tokens, usage);
    }

    private OrganizationService service() {
        return new OrganizationService(organizations, actor, employees, departments, tokens, usage, users,
                roles, positions, cashBoxes, permissions, sites);
    }

    private UserAccount user() {
        UserAccount user = new UserAccount("owner@example.com", "hash");
        user.setId(UUID.randomUUID());
        return user;
    }

    private RegistrationRequest request() {
        return new RegistrationRequest("Owner User", "owner@example.com", "password", null, "New Org", null, null);
    }
}
