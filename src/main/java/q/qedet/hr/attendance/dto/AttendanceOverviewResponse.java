package q.qedet.hr.attendance.dto;

import q.qedet.hr.attendance.model.AttendanceStatus;
import java.time.Instant;
import java.util.UUID;

public record AttendanceOverviewResponse(UUID employeeId, String employeeName, UUID departmentId,
                                         String departmentName, UUID scheduleId, String scheduleName,
                                         UUID locationId, Instant checkInAt, Instant checkOutAt,
                                         int lateMinutes, int earlyLeaveMinutes, int overtimeMinutes,
                                         AttendanceStatus status) {
}
