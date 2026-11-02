package q.qedet.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import q.qedet.common.model.RoleType;

import java.time.LocalDate;
import java.util.UUID;

public record EmployeeRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        String fullName,
        @NotBlank @Email String email,
        String phone,
        @NotNull UUID departmentId,
        @NotBlank String role,
        LocalDate hireDate,
        String status,
        Long salary,
        Boolean isActive
) {
}
