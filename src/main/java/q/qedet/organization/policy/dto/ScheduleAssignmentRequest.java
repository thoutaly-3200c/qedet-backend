package q.qedet.organization.policy.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ScheduleAssignmentRequest(
        @NotNull UUID scheduleId,
        @NotEmpty List<UUID> employeeIds,
        @NotNull LocalDate effectiveFrom,
        LocalDate effectiveTo
) {
}
