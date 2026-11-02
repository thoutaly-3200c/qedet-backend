package q.qedet.employee.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import q.qedet.auth.security.PermissionCatalog;
import q.qedet.common.model.BusinessType;
import q.qedet.common.model.RoleType;
import q.qedet.employee.entity.Role;
import q.qedet.employee.entity.RolePermission;
import q.qedet.employee.repository.PermissionRepository;
import q.qedet.employee.repository.RolePermissionRepository;
import q.qedet.employee.repository.RoleRepository;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.repository.OrganizationRepository;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultRolePermissionProvisioningServiceTest {

    @Mock private OrganizationRepository organizations;
    @Mock private RoleRepository roles;
    @Mock private PermissionRepository permissions;
    @Mock private RolePermissionRepository rolePermissions;

    private DefaultRolePermissionProvisioningService service;
    private Organization organization;
    private final Map<RoleType, Role> defaultRoles = new EnumMap<>(RoleType.class);

    @BeforeEach
    void setUp() {
        service = new DefaultRolePermissionProvisioningService(organizations, roles, permissions, rolePermissions);
        organization = new Organization("RBAC Test", BusinessType.GENERAL_TRADE);
        organization.setId(UUID.randomUUID());
        for (RoleType type : List.of(
                RoleType.FINANCE_MANAGER, RoleType.FINANCE_EMPLOYEE, RoleType.PROCUREMENT_MANAGER,
                RoleType.PROCUREMENT_EMPLOYEE, RoleType.WAREHOUSE_MANAGER, RoleType.WAREHOUSE_EMPLOYEE)) {
            Role role = new Role();
            role.setId(UUID.randomUUID());
            role.setCode(type.name());
            role.setName(type.name());
            role.setOrganization(organization);
            defaultRoles.put(type, role);
        }
        when(roles.findAllByOrganizationId(organization.getId()))
                .thenReturn(new ArrayList<>(defaultRoles.values()));
        when(permissions.findByCodeIgnoreCase(anyString())).thenAnswer(invocation -> {
            q.qedet.employee.entity.Permission permission = new q.qedet.employee.entity.Permission();
            permission.setId(UUID.randomUUID());
            permission.setCode(invocation.getArgument(0));
            return Optional.of(permission);
        });
    }

    @Test
    void reconcilesMissingBaselinePermissionsWithoutRemovingCustomPermissions() {
        when(rolePermissions.existsByRoleIdAndPermissionId(any(), any())).thenReturn(false);

        service.reconcile(organization);

        ArgumentCaptor<RolePermission> assignments = ArgumentCaptor.forClass(RolePermission.class);
        verify(rolePermissions, times(defaultPermissionCount())).save(assignments.capture());
        assertThat(assignments.getAllValues())
                .allMatch(assignment -> defaultRoles.containsValue(assignment.getRole()));
        verify(rolePermissions, never()).delete(any(RolePermission.class));
        verify(roles, never()).save(any(Role.class));
    }

    @Test
    void reconciliationIsIdempotentAndMapsLegacyAccountantToFinanceEmployee() {
        Role legacyAccountant = defaultRoles.remove(RoleType.FINANCE_EMPLOYEE);
        legacyAccountant.setCode("ACCOUNTANT");
        legacyAccountant.setName("Accountant");
        List<Role> legacyRoles = new ArrayList<>(defaultRoles.values());
        legacyRoles.add(legacyAccountant);
        when(roles.findAllByOrganizationId(organization.getId())).thenReturn(legacyRoles);
        when(rolePermissions.existsByRoleIdAndPermissionId(any(), any())).thenReturn(true);

        service.reconcile(organization);
        service.reconcile(organization);

        verify(roles, never()).save(any(Role.class));
        verify(rolePermissions, never()).save(any(RolePermission.class));
        verify(rolePermissions, never()).delete(any(RolePermission.class));
    }

    @Test
    void createsMissingCanonicalDefaultRolesForAnExistingOrganization() {
        when(roles.findAllByOrganizationId(organization.getId())).thenReturn(new ArrayList<>());
        when(roles.save(any(Role.class))).thenAnswer(invocation -> {
            Role role = invocation.getArgument(0);
            role.setId(UUID.randomUUID());
            return role;
        });
        when(rolePermissions.existsByRoleIdAndPermissionId(any(), any())).thenReturn(false);

        service.reconcile(organization);

        ArgumentCaptor<Role> createdRoles = ArgumentCaptor.forClass(Role.class);
        verify(roles, times(6)).save(createdRoles.capture());
        assertThat(createdRoles.getAllValues())
                .extracting(Role::getCode)
                .containsExactlyInAnyOrder(
                        RoleType.FINANCE_MANAGER.name(),
                        RoleType.FINANCE_EMPLOYEE.name(),
                        RoleType.PROCUREMENT_MANAGER.name(),
                        RoleType.PROCUREMENT_EMPLOYEE.name(),
                        RoleType.WAREHOUSE_MANAGER.name(),
                        RoleType.WAREHOUSE_EMPLOYEE.name());
        verify(rolePermissions, times(defaultPermissionCount())).save(any(RolePermission.class));
    }

    private int defaultPermissionCount() {
        return defaultRoles.keySet().stream()
                .mapToInt(type -> PermissionCatalog.permissionsFor(type).size())
                .sum();
    }
}
