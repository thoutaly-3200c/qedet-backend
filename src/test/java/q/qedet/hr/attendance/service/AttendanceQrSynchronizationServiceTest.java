package q.qedet.hr.attendance.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import q.qedet.hr.attendance.entity.AttendanceQr;
import q.qedet.hr.attendance.entity.AttendanceQrToken;
import q.qedet.hr.attendance.model.AttendanceQrTokenStatus;
import q.qedet.hr.attendance.repository.AttendanceQrRepository;
import q.qedet.hr.attendance.repository.AttendanceQrTokenRepository;
import q.qedet.organization.entity.Department;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.policy.entity.DepartmentScheduleAssignment;
import q.qedet.organization.policy.entity.WorkSchedule;
import q.qedet.organization.policy.model.AssignmentStatus;
import q.qedet.organization.policy.repository.DepartmentScheduleAssignmentRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceQrSynchronizationServiceTest {
    @Mock private AttendanceQrRepository attendanceQrs;
    @Mock private AttendanceQrTokenRepository qrTokens;
    @Mock private DepartmentScheduleAssignmentRepository assignments;

    @Test
    void updatesAnExistingQrFromAnActiveDepartmentScheduleAssignment() {
        DepartmentScheduleAssignment assignment = assignment(AssignmentStatus.ACTIVE);
        AttendanceQr existingQr = new AttendanceQr();
        existingQr.setSite(new q.qedet.organization.entity.OrganizationSite());
        when(attendanceQrs.findByOrganizationIdAndDepartmentId(
                assignment.getOrganization().getId(), assignment.getDepartment().getId())).thenReturn(Optional.of(existingQr));

        service().synchronize(assignment);

        ArgumentCaptor<AttendanceQr> captured = ArgumentCaptor.forClass(AttendanceQr.class);
        verify(attendanceQrs).save(captured.capture());
        AttendanceQr qr = captured.getValue();
        assertThat(qr.getSchedule()).isSameAs(assignment.getSchedule());
        assertThat(qr.getSite()).isSameAs(existingQr.getSite());
        assertThat(qr.isActive()).isTrue();
    }

    @Test
    void deactivatingAnAssignmentRevokesTodaysActiveQrToken() {
        DepartmentScheduleAssignment assignment = assignment(AssignmentStatus.ENDED);
        AttendanceQr qr = new AttendanceQr();
        qr.setId(UUID.randomUUID());
        qr.setSchedule(assignment.getSchedule());
        qr.setActive(true);
        AttendanceQrToken token = new AttendanceQrToken();
        token.setStatus(AttendanceQrTokenStatus.ACTIVE);
        when(attendanceQrs.findByOrganizationIdAndDepartmentId(
                assignment.getOrganization().getId(), assignment.getDepartment().getId())).thenReturn(Optional.of(qr));
        when(qrTokens.findByAttendanceQrIdAndWorkDate(eq(qr.getId()), any(LocalDate.class))).thenReturn(Optional.of(token));

        service().synchronize(assignment);

        assertThat(qr.isActive()).isFalse();
        assertThat(token.getStatus()).isEqualTo(AttendanceQrTokenStatus.REVOKED);
        verify(attendanceQrs).save(qr);
    }

    private AttendanceQrSynchronizationService service() {
        return new AttendanceQrSynchronizationService(attendanceQrs, qrTokens, assignments);
    }

    private DepartmentScheduleAssignment assignment(AssignmentStatus status) {
        Organization organization = new Organization();
        organization.setId(UUID.randomUUID());
        Department department = new Department();
        department.setId(UUID.randomUUID());
        WorkSchedule schedule = new WorkSchedule();
        schedule.setTimezone("UTC");
        DepartmentScheduleAssignment assignment = new DepartmentScheduleAssignment();
        assignment.setOrganization(organization);
        assignment.setDepartment(department);
        assignment.setSchedule(schedule);
        assignment.setStatus(status);
        return assignment;
    }
}
