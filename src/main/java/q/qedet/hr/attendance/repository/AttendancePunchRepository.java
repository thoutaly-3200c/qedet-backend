package q.qedet.hr.attendance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.hr.attendance.entity.AttendancePunch;
import java.util.UUID;

public interface AttendancePunchRepository extends JpaRepository<AttendancePunch, UUID> {}
