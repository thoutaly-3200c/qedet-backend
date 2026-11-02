package q.qedet.employee.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EmployeePositionAssignmentRequest(@NotNull UUID positionId, UUID locationId) {
}
