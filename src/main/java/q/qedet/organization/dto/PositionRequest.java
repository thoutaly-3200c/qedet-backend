package q.qedet.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record PositionRequest(
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 160) String name,
        @NotNull UUID departmentId,
        UUID defaultSiteId,
        @Size(max = 500) String description,
        Boolean active
) {
}
