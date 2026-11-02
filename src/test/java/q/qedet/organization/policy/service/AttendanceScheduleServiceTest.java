package q.qedet.organization.policy.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import q.qedet.auth.security.ActorContext;
import q.qedet.employee.entity.Employee;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.organization.entity.Department;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.policy.entity.WorkSchedule;
import q.qedet.organization.policy.model.WorkScheduleStatus;
import q.qedet.organization.policy.repository.DepartmentScheduleAssignmentRepository;
import q.qedet.organization.policy.repository.EmployeeWorkScheduleRepository;
import q.qedet.organization.policy.repository.WorkScheduleRepository;
import q.qedet.organization.repository.DepartmentRepository;
import q.qedet.hr.attendance.service.AttendanceQrSynchronizationService;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceScheduleServiceTest {

    @Mock private WorkScheduleRepository schedules;
    @Mock private EmployeeWorkScheduleRepository employeeAssignments;
    @Mock private DepartmentScheduleAssignmentRepository departmentAssignments;
    @Mock private EmployeeRepository employees;
    @Mock private DepartmentRepository departments;
    @Mock private AttendanceQrSynchronizationService attendanceQrs;
    @Mock private ActorContext actor;

    @Test
    void resolvesAnEmployeesScheduleFromTheirDepartment() {
        Organization organization = new Organization();
        Department department = new Department();
        WorkSchedule schedule = new WorkSchedule();
        schedule.setName("Weekdays");
        department.setSchedule(schedule);
        Employee employee = new Employee();
        employee.setOrganization(organization);
        employee.setDepartment(department);
        AttendanceScheduleService service = new AttendanceScheduleService(
                schedules, employeeAssignments, departmentAssignments, employees, departments,
                attendanceQrs, actor);
        when(employeeAssignments.findAllByOrganizationIdAndEmployeeId(null, null)).thenReturn(java.util.List.of());
        when(departmentAssignments.findAllByOrganizationIdAndDepartmentId(null, null)).thenReturn(java.util.List.of());

        WorkSchedule resolved = service.scheduleFor(employee, LocalDate.now());

        assertThat(resolved).isSameAs(schedule);
    }

    @Test
    void restoresAnArchivedScheduleToDraft() {
        UUID organizationId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(organizationId);
        WorkSchedule schedule = new WorkSchedule();
        schedule.setId(scheduleId);
        schedule.setOrganization(organization);
        schedule.setName("Archived schedule");
        schedule.setStatus(WorkScheduleStatus.ARCHIVED);
        AttendanceScheduleService service = service();
        when(actor.organizationId()).thenReturn(organizationId);
        when(schedules.findByIdAndOrganizationId(scheduleId, organizationId)).thenReturn(Optional.of(schedule));

        assertThat(service.restore(organizationId, scheduleId).status()).isEqualTo(WorkScheduleStatus.DRAFT);

        verify(actor).requirePermission(q.qedet.common.model.Permission.ATTENDANCE_SCHEDULE_UPDATE);
    }

    @Test
    void doesNotRestoreSchedulesThatAreNotArchived() {
        UUID organizationId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(organizationId);
        WorkSchedule schedule = new WorkSchedule();
        schedule.setId(scheduleId);
        schedule.setOrganization(organization);
        schedule.setName("Active schedule");
        schedule.setStatus(WorkScheduleStatus.ACTIVE);
        AttendanceScheduleService service = service();
        when(actor.organizationId()).thenReturn(organizationId);
        when(schedules.findByIdAndOrganizationId(scheduleId, organizationId)).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> service.restore(organizationId, scheduleId))
                .isInstanceOf(q.qedet.common.exception.ConflictException.class);
    }

    private AttendanceScheduleService service() {
        return new AttendanceScheduleService(
                schedules, employeeAssignments, departmentAssignments, employees, departments,
                attendanceQrs, actor);
    }
}
