package q.qedet.hr.attendance.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.hr.attendance.entity.AttendanceQr;
import q.qedet.hr.attendance.model.AttendanceQrTokenStatus;
import q.qedet.hr.attendance.repository.AttendanceQrRepository;
import q.qedet.hr.attendance.repository.AttendanceQrTokenRepository;
import q.qedet.organization.policy.entity.DepartmentScheduleAssignment;
import q.qedet.organization.policy.model.AssignmentStatus;
import q.qedet.organization.policy.repository.DepartmentScheduleAssignmentRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class AttendanceQrSynchronizationService {
    private final AttendanceQrRepository attendanceQrs;
    private final AttendanceQrTokenRepository qrTokens;
    private final DepartmentScheduleAssignmentRepository assignments;

    public AttendanceQrSynchronizationService(AttendanceQrRepository attendanceQrs,
                                              AttendanceQrTokenRepository qrTokens,
                                              DepartmentScheduleAssignmentRepository assignments) {
        this.attendanceQrs = attendanceQrs;
        this.qrTokens = qrTokens;
        this.assignments = assignments;
    }

    @Transactional
    public void synchronize(DepartmentScheduleAssignment assignment) {
        AttendanceQr qr = attendanceQrs.findByOrganizationIdAndDepartmentId(
                assignment.getOrganization().getId(), assignment.getDepartment().getId()).orElse(null);
        if (assignment.getStatus() != AssignmentStatus.ACTIVE) {
            if (qr != null && qr.isActive()) {
                deactivate(qr);
            }
            return;
        }

        if (qr != null) {
            qr.setSchedule(assignment.getSchedule());
            qr.setActive(true);
            attendanceQrs.save(qr);
        }
    }

    @Transactional
    public void synchronizeOrganization(UUID organizationId) {
        List<DepartmentScheduleAssignment> organizationAssignments = assignments.findAllByOrganizationId(organizationId);
        organizationAssignments.forEach(this::synchronize);
        attendanceQrs.findAllByOrganizationId(organizationId).stream()
                .filter(AttendanceQr::isActive)
                .filter(qr -> organizationAssignments.stream().noneMatch(assignment ->
                        assignment.getStatus() == AssignmentStatus.ACTIVE
                                && assignment.getDepartment().getId().equals(qr.getDepartment().getId())))
                .forEach(this::deactivate);
    }

    private void deactivate(AttendanceQr qr) {
        qr.setActive(false);
        qrTokens.findByAttendanceQrIdAndWorkDate(qr.getId(), LocalDate.now(zone(qr)))
                .filter(token -> token.getStatus() == AttendanceQrTokenStatus.ACTIVE)
                .ifPresent(token -> token.setStatus(AttendanceQrTokenStatus.REVOKED));
        attendanceQrs.save(qr);
    }

    private ZoneId zone(AttendanceQr qr) {
        String timezone = qr.getSchedule().getTimezone();
        return ZoneId.of(timezone == null || timezone.isBlank() ? "UTC" : timezone);
    }
}
