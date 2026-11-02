package q.qedet.employee.dto;



import q.qedet.common.model.EmployeeStatus;
import q.qedet.common.model.RoleType;

import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

public record EmployeeResponse(
        UUID id,
        UUID organizationId,
        UUID userId,
        String employeeNumber,
        String firstName,
        String lastName,
        String fullName,
        String email,
        String phone,
        UUID departmentId,
        PositionReference position,
        DepartmentReference department,
        LocationReference location,
        RoleType role,
        EmployeeStatus status,
        LocalDate hireDate,
        Long salary,
        Boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
    public record PositionReference(UUID id, String code, String name) {}
    public record DepartmentReference(UUID id, String name) {}
    public record LocationReference(UUID id, String code, String name) {}
}
