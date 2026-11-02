package q.qedet.auth.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import q.qedet.common.model.Permission;
import q.qedet.common.model.RoleType;
import q.qedet.common.exception.ForbiddenException;
import q.qedet.common.exception.UnauthorizedException;

import java.util.Set;
import java.util.UUID;

@Component
public class ActorContext {

    public AuthenticatedPrincipal current() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedPrincipal principal)) {
            throw new UnauthorizedException("Authentication required");
        }
        return principal;
    }

    public UUID organizationId() {
        return current().organizationId();
    }

    public UUID employeeId() {
        return current().employeeId();
    }

    public UUID userId() {
        return current().userId();
    }

    public RoleType role() {
        return current().role();
    }

    public Set<Permission> permissions() {
        return current().permissions();
    }

    public void requirePermission(Permission permission) {
        if (!permissions().contains(permission)) {
            throw new ForbiddenException("Missing permission: " + permission);
        }
    }

    public void requireRoleCanAssign(RoleType targetRole) {
        if (!role().canAssign(targetRole)) {
            throw new ForbiddenException("Role " + role() + " cannot assign " + targetRole);
        }
    }
}
