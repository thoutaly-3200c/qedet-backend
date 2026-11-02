package q.qedet.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record DepartmentRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 500) String description,
        @NotBlank @Size(max = 40) String code,
        UUID parentDepartmentId,
        UUID scheduleId
) {
    public DepartmentRequest(String name, String code) {
        this(name, null, code, null, null);
    }
}
