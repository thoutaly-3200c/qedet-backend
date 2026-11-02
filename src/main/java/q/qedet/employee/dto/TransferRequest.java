package q.qedet.employee.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record TransferRequest(
        @NotNull UUID departmentId,
        UUID managerId,
        LocalDate effectiveDate,
        String reason
) {
}
