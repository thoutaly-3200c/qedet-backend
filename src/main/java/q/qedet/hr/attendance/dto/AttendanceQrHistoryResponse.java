package q.qedet.hr.attendance.dto;

import q.qedet.hr.attendance.model.AttendanceQrTokenStatus;
import java.time.Instant;
import java.time.LocalDate;

public record AttendanceQrHistoryResponse(LocalDate workDate, Instant validFrom, Instant validUntil,
                                          AttendanceQrTokenStatus status) {
}
