package q.qedet.employee.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.ConflictException;
import q.qedet.common.exception.NotFoundException;
import q.qedet.common.model.Permission;
import q.qedet.employee.dto.*;
import q.qedet.employee.entity.*;
import q.qedet.employee.repository.*;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.repository.OrganizationRepository;

import java.util.Set;
import java.util.UUID;

@Service
public class RoleService {
    private final RoleRepository roles;
    private final PermissionRepository permissions;
    private final RolePermissionRepository rolePermissions;
    private final OrganizationRepository organizations;
    private final ActorContext actor;

    public RoleService(RoleRepository roles, PermissionRepository permissions, RolePermissionRepository rolePermissions,
                       OrganizationRepository organizations, ActorContext actor) {
        this.roles = roles; this.permissions = permissions; this.rolePermissions = rolePermissions;
        this.organizations = organizations; this.actor = actor;
    }

    @Transactional(readOnly = true)
    public java.util.List<RoleResponse> list() {
        actor.requirePermission(Permission.EMPLOYEE_VIEW);
        return roles.findAllByOrganizationId(actor.organizationId()).stream().map(this::response).toList();
    }

    @Transactional
    public RoleResponse create(RoleRequest request) {
        actor.requirePermission(Permission.EMPLOYEE_MANAGE);
        String name = request.name().trim();
        if (roles.existsByOrganizationIdAndNameIgnoreCase(actor.organizationId(), name))
            throw new ConflictException("Role name already exists");
        Organization organization = organizations.findById(actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Organization not found"));
        Role role = new Role(); role.setOrganization(organization); role.setName(name);
        role.setDescription(request.description()); role.setSystemRole(false);
        return response(roles.save(role));
    }

    @Transactional
    public RoleResponse update(UUID id, RoleRequest request) {
        actor.requirePermission(Permission.EMPLOYEE_MANAGE);
        Role role = owned(id); role.setName(request.name().trim()); role.setDescription(request.description());
        return response(roles.save(role));
    }

    @Transactional
    public RoleResponse addPermission(UUID roleId, String code) {
        actor.requirePermission(Permission.EMPLOYEE_MANAGE);
        Role role = owned(roleId);
        q.qedet.employee.entity.Permission permission = permissions.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new NotFoundException("Permission not found"));
        if (rolePermissions.findByRoleIdAndPermissionId(roleId, permission.getId()).isEmpty()) {
            RolePermission assignment = new RolePermission(); assignment.setRole(role); assignment.setPermission(permission);
            rolePermissions.save(assignment);
        }
        return response(role);
    }

    @Transactional
    public void removePermission(UUID roleId, UUID permissionId) {
        actor.requirePermission(Permission.EMPLOYEE_MANAGE);
        owned(roleId);
        rolePermissions.findByRoleIdAndPermissionId(roleId, permissionId).ifPresent(rolePermissions::delete);
    }

    private Role owned(UUID id) {
        return roles.findByIdAndOrganizationId(id, actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Role not found"));
    }

    private RoleResponse response(Role role) {
        Set<String> assigned = rolePermissions.findAll().stream()
                .filter(value -> value.getRole().getId().equals(role.getId()))
                .map(value -> value.getPermission().getCode()).collect(java.util.stream.Collectors.toSet());
        return new RoleResponse(role.getId(), role.getOrganization().getId(), role.getName(), role.getDescription(),
                role.isSystemRole(), assigned, role.getCreatedAt(), role.getUpdatedAt());
    }
}
