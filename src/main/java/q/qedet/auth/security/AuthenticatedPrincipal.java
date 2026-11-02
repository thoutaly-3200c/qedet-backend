package q.qedet.auth.security;



import q.qedet.common.model.Permission;
import q.qedet.common.model.RoleType;

import java.util.Set;
import java.util.UUID;

public record AuthenticatedPrincipal(
        UUID userId,
        UUID employeeId,
        UUID organizationId,
        RoleType role,
        Set<Permission> permissions,
        String email
) {
}
