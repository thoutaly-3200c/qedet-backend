package q.qedet.organization.dto;



import q.qedet.common.model.DepartmentStatus;

import java.time.Instant;
import java.util.UUID;

public record DepartmentResponse(
        UUID id,
        UUID organizationId,
        String name,
        String code,
        String description,
        UUID parentDepartmentId,
        UUID scheduleId,
        String scheduleName,
        DepartmentStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
