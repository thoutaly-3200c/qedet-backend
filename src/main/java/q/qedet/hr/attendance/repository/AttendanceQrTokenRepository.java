package q.qedet.hr.attendance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.hr.attendance.entity.AttendanceQrToken;
import q.qedet.hr.attendance.model.AttendanceQrTokenStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceQrTokenRepository extends JpaRepository<AttendanceQrToken, UUID> {
    Optional<AttendanceQrToken> findByAttendanceQrIdAndWorkDate(UUID attendanceQrId, LocalDate workDate);
    Optional<AttendanceQrToken> findByTokenHashAndStatus(String tokenHash, AttendanceQrTokenStatus status);
    List<AttendanceQrToken> findAllByAttendanceQrIdOrderByWorkDateDesc(UUID attendanceQrId);
    List<AttendanceQrToken> findAllByStatusAndWorkDateBefore(AttendanceQrTokenStatus status, LocalDate workDate);
    List<AttendanceQrToken> findAllByStatus(AttendanceQrTokenStatus status);
}
