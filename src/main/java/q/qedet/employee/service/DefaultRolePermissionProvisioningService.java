package q.qedet.employee.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.auth.security.PermissionCatalog;
import q.qedet.common.model.Permission;
import q.qedet.common.model.RoleType;
import q.qedet.employee.entity.Role;
import q.qedet.employee.entity.RolePermission;
import q.qedet.employee.repository.PermissionRepository;
import q.qedet.employee.repository.RolePermissionRepository;
import q.qedet.employee.repository.RoleRepository;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.repository.OrganizationRepository;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Adds missing baseline grants without removing organization-specific grants.
 */
@Component
@Order(1)
public class DefaultRolePermissionProvisioningService implements CommandLineRunner {

    private static final Map<RoleType, String> ROLE_NAMES = new EnumMap<>(RoleType.class);

    static {
        ROLE_NAMES.put(RoleType.FINANCE_MANAGER, "Finance Manager");
        ROLE_NAMES.put(RoleType.FINANCE_EMPLOYEE, "Finance Employee");
        ROLE_NAMES.put(RoleType.PROCUREMENT_MANAGER, "Procurement Manager");
        ROLE_NAMES.put(RoleType.PROCUREMENT_EMPLOYEE, "Procurement Employee");
        ROLE_NAMES.put(RoleType.WAREHOUSE_MANAGER, "Warehouse Manager");
        ROLE_NAMES.put(RoleType.WAREHOUSE_EMPLOYEE, "Warehouse Employee");
    }

    private final OrganizationRepository organizations;
    private final RoleRepository roles;
    private final PermissionRepository permissions;
    private final RolePermissionRepository rolePermissions;

    public DefaultRolePermissionProvisioningService(OrganizationRepository organizations, RoleRepository roles,
                                                    PermissionRepository permissions,
                                                    RolePermissionRepository rolePermissions) {
        this.organizations = organizations;
        this.roles = roles;
        this.permissions = permissions;
        this.rolePermissions = rolePermissions;
    }

    @Override
    @Transactional
    public void run(String... args) {
        organizations.findAll().forEach(this::reconcile);
    }

    @Transactional
    public void reconcile(Organization organization) {
        List<Role> organizationRoles = roles.findAllByOrganizationId(organization.getId());
        for (RoleType type : ROLE_NAMES.keySet()) {
            Role role = resolveRole(organization, organizationRoles, type);
            if (role != null) {
                grantMissingDefaults(role, type);
            }
        }
    }

    private Role resolveRole(Organization organization, List<Role> organizationRoles, RoleType type) {
        String code = type.name();
        Role role = organizationRoles.stream()
                .filter(candidate -> code.equalsIgnoreCase(candidate.getCode()))
                .findFirst()
                .orElse(null);
        if (role != null) {
            return role;
        }
        if (type == RoleType.FINANCE_EMPLOYEE) {
            role = organizationRoles.stream()
                    .filter(candidate -> "ACCOUNTANT".equalsIgnoreCase(candidate.getCode()))
                    .findFirst()
                    .orElse(null);
            if (role != null) {
                return role;
            }
        }
        String name = ROLE_NAMES.get(type);
        if (organizationRoles.stream().anyMatch(candidate -> name.equalsIgnoreCase(candidate.getName()))) {
            return null;
        }
        Role created = new Role();
        created.setOrganization(organization);
        created.setName(name);
        created.setCode(code);
        created.setSystemRole(true);
        return roles.save(created);
    }

    private void grantMissingDefaults(Role role, RoleType type) {
        for (Permission code : PermissionCatalog.permissionsFor(type)) {
            q.qedet.employee.entity.Permission permission = permissions.findByCodeIgnoreCase(code.name())
                    .orElseThrow(() -> new IllegalStateException("Permission definition is missing for code " + code));
            if (!rolePermissions.existsByRoleIdAndPermissionId(role.getId(), permission.getId())) {
                RolePermission assignment = new RolePermission();
                assignment.setRole(role);
                assignment.setPermission(permission);
                rolePermissions.save(assignment);
            }
        }
    }
}
