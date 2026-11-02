package q.qedet.employee.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RoleAssignmentRequest(@NotNull UUID roleId) {}
