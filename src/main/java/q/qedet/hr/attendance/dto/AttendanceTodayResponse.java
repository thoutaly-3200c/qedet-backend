package q.qedet.hr.attendance.dto;

import q.qedet.hr.attendance.model.AttendanceTodayStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record AttendanceTodayResponse(
        LocalDate date,
        AttendanceTodayStatus status,
        boolean hasSchedule,
        boolean dayOff,
        String department,
        String scheduleName,
        Schedule schedule,
        String location,
        Instant checkIn,
        Instant checkOut
) {
    public record Schedule(LocalTime startTime, LocalTime endTime) {
    }
}
