package q.qedet.hr.attendance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.hr.attendance.entity.AttendanceRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {
    Optional<AttendanceRecord> findByEmployeeIdAndWorkDate(UUID employeeId, LocalDate date);

    Optional<AttendanceRecord> findByOrganizationIdAndEmployeeIdAndWorkDate(UUID organizationId,
                                                                           UUID employeeId,
                                                                           LocalDate date);

    List<AttendanceRecord> findAllByOrganizationIdAndWorkDateBetween(UUID organizationId,
                                                                     LocalDate from,
                                                                     LocalDate to);
    List<AttendanceRecord> findAllByOrganizationIdAndEmployeeIdAndWorkDateBetween(UUID organizationId,
                                                                                  UUID employeeId,
                                                                                  LocalDate from,
                                                                                  LocalDate to);
    boolean existsBySiteId(UUID siteId);
}
