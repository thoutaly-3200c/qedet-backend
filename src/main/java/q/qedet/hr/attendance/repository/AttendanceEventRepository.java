package q.qedet.hr.attendance.repository;
import org.springframework.data.jpa.repository.JpaRepository; import q.qedet.hr.attendance.entity.AttendanceEvent; import q.qedet.hr.attendance.model.AttendanceEventType; import java.util.*;
public interface AttendanceEventRepository extends JpaRepository<AttendanceEvent,UUID> {
    boolean existsByAttendanceRecordIdAndType(UUID attendanceId, AttendanceEventType type);
    boolean existsBySiteId(UUID siteId);
}
