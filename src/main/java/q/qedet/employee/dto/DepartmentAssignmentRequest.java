package q.qedet.employee.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record DepartmentAssignmentRequest(@NotNull UUID departmentId, boolean primary) {}
