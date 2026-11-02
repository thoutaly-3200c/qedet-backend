package q.qedet.hr.attendance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.hr.attendance.entity.AttendanceQr;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceQrRepository extends JpaRepository<AttendanceQr, UUID> {
    Optional<AttendanceQr> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Optional<AttendanceQr> findByOrganizationIdAndDepartmentId(UUID organizationId, UUID departmentId);
    List<AttendanceQr> findAllByOrganizationId(UUID organizationId);
    List<AttendanceQr> findAllByActiveTrue();
    boolean existsByOrganizationIdAndDepartmentIdAndActiveTrue(UUID organizationId, UUID departmentId);
    boolean existsBySiteId(UUID siteId);
}
