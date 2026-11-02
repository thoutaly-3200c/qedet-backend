package q.qedet.employee.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record RoleResponse(UUID id, UUID organizationId, String name, String description,
                           boolean systemRole, Set<String> permissions,
                           Instant createdAt, Instant updatedAt) {}
