package q.qedet.hr.attendance.dto;

import java.util.UUID;

public record AttendanceQrResponse(UUID id, UUID departmentId, String departmentName,
                                   UUID scheduleId, String scheduleName,
                                   UUID locationId, String locationName, boolean active) {
}
