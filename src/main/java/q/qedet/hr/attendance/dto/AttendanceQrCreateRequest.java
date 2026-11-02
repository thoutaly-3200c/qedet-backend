package q.qedet.hr.attendance.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AttendanceQrCreateRequest(
        @NotNull UUID departmentId,
        @NotNull UUID scheduleId,
        @NotNull UUID locationId) {
}
