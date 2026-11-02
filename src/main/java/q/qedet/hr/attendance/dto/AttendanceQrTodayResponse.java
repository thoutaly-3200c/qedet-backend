package q.qedet.hr.attendance.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AttendanceQrTodayResponse(UUID qrId, UUID departmentId, String departmentName,
                                        UUID scheduleId, String scheduleName, UUID locationId, String locationName,
                                        LocalDate workDate, String qrToken, Instant validFrom, Instant validUntil,
                                        String status) {
}
