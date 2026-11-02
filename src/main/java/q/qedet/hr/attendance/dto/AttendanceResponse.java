package q.qedet.hr.attendance.dto;

import q.qedet.hr.attendance.model.AttendanceStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AttendanceResponse(
        UUID id,
        UUID employeeId,
        LocalDate workDate,
        AttendanceStatus status,
        Instant checkInAt,
        Instant checkOutAt,
        Integer workedMinutes,
        Integer overtimeMinutes,
        boolean authorizedLeave,
        Integer scheduledMinutes,
        Integer lateMinutes,
        Integer earlyLeaveMinutes,
        UUID locationId
) {
}

