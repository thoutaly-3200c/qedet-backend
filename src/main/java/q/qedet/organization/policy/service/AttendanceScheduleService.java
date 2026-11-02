package q.qedet.organization.policy.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.BadRequestException;
import q.qedet.common.exception.ConflictException;
import q.qedet.common.exception.ForbiddenException;
import q.qedet.common.exception.NotFoundException;
import q.qedet.common.model.Permission;
import q.qedet.employee.entity.Employee;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.organization.policy.dto.DepartmentScheduleAssignmentRequest;
import q.qedet.organization.policy.dto.DepartmentScheduleAssignmentResponse;
import q.qedet.organization.policy.dto.ScheduleAssignmentRequest;
import q.qedet.organization.policy.dto.ScheduleAssignmentResponse;
import q.qedet.organization.policy.dto.WorkScheduleRequest;
import q.qedet.organization.policy.dto.WorkScheduleResponse;
import q.qedet.organization.policy.entity.DepartmentScheduleAssignment;
import q.qedet.organization.policy.entity.EmployeeWorkSchedule;
import q.qedet.organization.policy.entity.WorkSchedule;
import q.qedet.organization.policy.model.AssignmentStatus;
import q.qedet.organization.policy.model.WorkScheduleStatus;
import q.qedet.organization.policy.repository.DepartmentScheduleAssignmentRepository;
import q.qedet.organization.policy.repository.EmployeeWorkScheduleRepository;
import q.qedet.organization.policy.repository.WorkScheduleRepository;
import q.qedet.organization.entity.Department;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.repository.DepartmentRepository;
import q.qedet.hr.attendance.service.AttendanceQrSynchronizationService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AttendanceScheduleService {
    private final WorkScheduleRepository schedules;
    private final EmployeeWorkScheduleRepository employeeAssignments;
    private final DepartmentScheduleAssignmentRepository departmentAssignments;
    private final EmployeeRepository employees;
    private final DepartmentRepository departments;
    private final AttendanceQrSynchronizationService attendanceQrs;
    private final ActorContext actor;

    public AttendanceScheduleService(WorkScheduleRepository schedules,
                                    EmployeeWorkScheduleRepository employeeAssignments,
                                    DepartmentScheduleAssignmentRepository departmentAssignments,
                                    EmployeeRepository employees,
                                    DepartmentRepository departments,
                                    AttendanceQrSynchronizationService attendanceQrs,
                                    ActorContext actor) {
        this.schedules = schedules;
        this.employeeAssignments = employeeAssignments;
        this.departmentAssignments = departmentAssignments;
        this.employees = employees;
        this.departments = departments;
        this.attendanceQrs = attendanceQrs;
        this.actor = actor;
    }

    private void ensureOrganization(UUID organizationId) {
        if (!actor.organizationId().equals(organizationId)) {
            throw new ForbiddenException("Organization access denied");
        }
    }

    public WorkScheduleResponse createForCurrentOrganization(WorkScheduleRequest request) {
        return create(actor.organizationId(), request);
    }

    public List<WorkScheduleResponse> listForCurrentOrganization() {
        return list(actor.organizationId());
    }

    public WorkScheduleResponse getForCurrentOrganization(UUID id) {
        return get(actor.organizationId(), id);
    }

    public WorkScheduleResponse updateForCurrentOrganization(UUID id, WorkScheduleRequest request) {
        return update(actor.organizationId(), id, request);
    }

    public WorkScheduleResponse archiveForCurrentOrganization(UUID id) {
        return archive(actor.organizationId(), id);
    }

    public WorkScheduleResponse restoreForCurrentOrganization(UUID id) {
        return restore(actor.organizationId(), id);
    }

    @Transactional
    public WorkScheduleResponse create(UUID organizationId, WorkScheduleRequest request) {
        ensureOrganization(organizationId);
        actor.requirePermission(Permission.ATTENDANCE_SCHEDULE_CREATE);

        WorkSchedule schedule = new WorkSchedule();
        schedule.setOrganization(new Organization());
        schedule.getOrganization().setId(organizationId);
        schedule.setName(request.name().trim());
        schedule.setDescription(request.description());
        schedule.setTimezone(request.timezone() == null ? "UTC" : request.timezone());
        schedule.setGracePeriodMinutes(request.gracePeriodMinutes() == null ? 0 : request.gracePeriodMinutes());
        schedule.setOrganizationDefault(Boolean.TRUE.equals(request.organizationDefault()));
        if (request.workdays() != null) {
            schedule.setWorkdays(request.workdays());
        }

        return response(schedules.save(schedule));
    }

    @Transactional(readOnly = true)
    public List<WorkScheduleResponse> list(UUID organizationId) {
        ensureOrganization(organizationId);
        actor.requirePermission(Permission.ATTENDANCE_VIEW);
        return schedules.findAllByOrganizationId(organizationId).stream()
                .map(this::response)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkScheduleResponse get(UUID organizationId, UUID id) {
        ensureOrganization(organizationId);
        actor.requirePermission(Permission.ATTENDANCE_VIEW);
        return response(schedule(organizationId, id));
    }

    @Transactional
    public WorkScheduleResponse update(UUID organizationId, UUID id, WorkScheduleRequest request) {
        ensureOrganization(organizationId);
        actor.requirePermission(Permission.ATTENDANCE_SCHEDULE_UPDATE);

        WorkSchedule schedule = schedule(organizationId, id);
        if (schedule.getStatus() == WorkScheduleStatus.ARCHIVED) {
            throw new ConflictException("Schedule is archived");
        }

        schedule.setName(request.name().trim());
        schedule.setDescription(request.description());
        schedule.setTimezone(request.timezone() == null ? schedule.getTimezone() : request.timezone());
        schedule.setGracePeriodMinutes(request.gracePeriodMinutes() == null ? schedule.getGracePeriodMinutes() : request.gracePeriodMinutes());
        if (request.workdays() != null) {
            schedule.setWorkdays(request.workdays());
        }

        return response(schedules.save(schedule));
    }

    @Transactional
    public WorkScheduleResponse activate(UUID organizationId, UUID id) {
        ensureOrganization(organizationId);
        actor.requirePermission(Permission.ATTENDANCE_SCHEDULE_ACTIVATE);

        WorkSchedule schedule = schedule(organizationId, id);
        if (schedule.getStatus() == WorkScheduleStatus.ARCHIVED) {
            throw new ConflictException("Archived schedule cannot be activated");
        }

        schedule.setStatus(WorkScheduleStatus.ACTIVE);
        return response(schedule);
    }

    @Transactional
    public WorkScheduleResponse archive(UUID organizationId, UUID id) {
        ensureOrganization(organizationId);
        actor.requirePermission(Permission.ATTENDANCE_SCHEDULE_ARCHIVE);

        WorkSchedule schedule = schedule(organizationId, id);
        schedule.setStatus(WorkScheduleStatus.ARCHIVED);
        return response(schedule);
    }

    @Transactional
    public WorkScheduleResponse restore(UUID organizationId, UUID id) {
        ensureOrganization(organizationId);
        actor.requirePermission(Permission.ATTENDANCE_SCHEDULE_UPDATE);

        WorkSchedule schedule = schedule(organizationId, id);
        if (schedule.getStatus() != WorkScheduleStatus.ARCHIVED) {
            throw new ConflictException("Only archived schedules can be restored");
        }

        schedule.setStatus(WorkScheduleStatus.DRAFT);
        return response(schedule);
    }

    @Transactional
    public List<ScheduleAssignmentResponse> assignEmployees(UUID organizationId, ScheduleAssignmentRequest request) {
        ensureOrganization(organizationId);
        actor.requirePermission(Permission.ATTENDANCE_SCHEDULE_ASSIGN);

        WorkSchedule schedule = schedule(organizationId, request.scheduleId());
        if (schedule.getStatus() != WorkScheduleStatus.ACTIVE) {
            throw new ConflictException("Schedule is not active");
        }
        if (request.effectiveTo() != null && request.effectiveTo().isBefore(request.effectiveFrom())) {
            throw new BadRequestException("Invalid effective dates");
        }

        List<ScheduleAssignmentResponse> result = new ArrayList<>();
        for (UUID employeeId : request.employeeIds()) {
            Employee employee = employees.findByIdAndOrganizationId(employeeId, organizationId)
                    .orElseThrow(() -> new NotFoundException("Employee not found"));

            for (EmployeeWorkSchedule existing : employeeAssignments.findAllByOrganizationIdAndEmployeeId(organizationId, employeeId)) {
                if (existing.getStatus() == AssignmentStatus.ACTIVE
                        && overlap(existing.getEffectiveFrom(), existing.getEffectiveTo(),
                        request.effectiveFrom(), request.effectiveTo())) {
                    throw new ConflictException("Schedule assignment conflict");
                }
            }

            EmployeeWorkSchedule assignment = new EmployeeWorkSchedule();
            assignment.setOrganization(employee.getOrganization());
            assignment.setEmployee(employee);
            assignment.setWorkSchedule(schedule);
            assignment.setEffectiveFrom(request.effectiveFrom());
            assignment.setEffectiveTo(request.effectiveTo());
            assignment.setAssignedBy(actor.employeeId());
            assignment.setAssignedAt(Instant.now());
            result.add(to(employeeAssignments.save(assignment)));
        }

        return result;
    }

    @Transactional
    public DepartmentScheduleAssignmentResponse assignDepartment(UUID organizationId,
                                                                 DepartmentScheduleAssignmentRequest request) {
        ensureOrganization(organizationId);
        actor.requirePermission(Permission.ATTENDANCE_SCHEDULE_ASSIGN);

        Department department = departments.findByIdAndOrganizationId(request.departmentId(), organizationId)
                .orElseThrow(() -> new NotFoundException("Department not found"));
        WorkSchedule schedule = schedule(organizationId, request.scheduleId());
        if (schedule.getStatus() != WorkScheduleStatus.ACTIVE) {
            throw new ConflictException("Schedule is not active");
        }
        if (request.effectiveTo() != null && request.effectiveTo().isBefore(request.effectiveFrom())) {
            throw new BadRequestException("Invalid effective dates");
        }
        for (DepartmentScheduleAssignment existing : departmentAssignments
                .findAllByOrganizationIdAndDepartmentId(organizationId, department.getId())) {
            if (existing.getStatus() == AssignmentStatus.ACTIVE
                    && overlap(existing.getEffectiveFrom(), existing.getEffectiveTo(),
                    request.effectiveFrom(), request.effectiveTo())) {
                throw new ConflictException("Schedule assignment conflict");
            }
        }

        DepartmentScheduleAssignment assignment = new DepartmentScheduleAssignment();
        assignment.setOrganization(department.getOrganization());
        assignment.setDepartment(department);
        assignment.setSchedule(schedule);
        assignment.setEffectiveFrom(request.effectiveFrom());
        assignment.setEffectiveTo(request.effectiveTo());
        assignment.setAssignedBy(actor.employeeId());
        assignment.setAssignedAt(Instant.now());
        DepartmentScheduleAssignment saved = departmentAssignments.save(assignment);
        department.setSchedule(schedule);
        departments.save(department);
        attendanceQrs.synchronize(saved);
        return response(saved);
    }


    @Transactional
    public DepartmentScheduleAssignmentResponse updateDepartmentAssignment(UUID organizationId, UUID assignmentId,
                                                                            DepartmentScheduleAssignmentRequest request) {
        ensureOrganization(organizationId);
        actor.requirePermission(Permission.ATTENDANCE_SCHEDULE_ASSIGN);
        DepartmentScheduleAssignment assignment = departmentAssignments.findByIdAndOrganizationId(assignmentId, organizationId)
                .orElseThrow(() -> new NotFoundException("Department schedule assignment not found"));
        WorkSchedule schedule = schedule(organizationId, request.scheduleId());
        if (schedule.getStatus() != WorkScheduleStatus.ACTIVE) {
            throw new ConflictException("Schedule is not active");
        }
        if (request.effectiveTo() != null && request.effectiveTo().isBefore(request.effectiveFrom())) {
            throw new BadRequestException("Invalid effective dates");
        }
        assignment.setSchedule(schedule);
        assignment.setEffectiveFrom(request.effectiveFrom());
        assignment.setEffectiveTo(request.effectiveTo());
        DepartmentScheduleAssignment saved = departmentAssignments.save(assignment);
        assignment.getDepartment().setSchedule(schedule);
        departments.save(assignment.getDepartment());
        attendanceQrs.synchronize(saved);
        return response(saved);
    }

    @Transactional
    public DepartmentScheduleAssignmentResponse endDepartmentAssignment(UUID organizationId, UUID assignmentId) {
        ensureOrganization(organizationId);
        actor.requirePermission(Permission.ATTENDANCE_SCHEDULE_ASSIGN);
        DepartmentScheduleAssignment assignment = departmentAssignments.findByIdAndOrganizationId(assignmentId, organizationId)
                .orElseThrow(() -> new NotFoundException("Department schedule assignment not found"));
        assignment.setStatus(AssignmentStatus.ENDED);
        DepartmentScheduleAssignment saved = departmentAssignments.save(assignment);
        attendanceQrs.synchronize(saved);
        return response(saved);
    }

    @Transactional
    public DepartmentScheduleAssignmentResponse activateDepartmentAssignment(UUID organizationId, UUID assignmentId) {
        ensureOrganization(organizationId);
        actor.requirePermission(Permission.ATTENDANCE_SCHEDULE_ASSIGN);
        DepartmentScheduleAssignment assignment = departmentAssignments.findByIdAndOrganizationId(assignmentId, organizationId)
                .orElseThrow(() -> new NotFoundException("Department schedule assignment not found"));
        if (assignment.getSchedule().getStatus() != WorkScheduleStatus.ACTIVE) {
            throw new ConflictException("Schedule is not active");
        }
        for (DepartmentScheduleAssignment existing : departmentAssignments.findAllByOrganizationIdAndDepartmentId(
                organizationId, assignment.getDepartment().getId())) {
            if (!existing.getId().equals(assignment.getId()) && existing.getStatus() == AssignmentStatus.ACTIVE
                    && overlap(existing.getEffectiveFrom(), existing.getEffectiveTo(),
                    assignment.getEffectiveFrom(), assignment.getEffectiveTo())) {
                throw new ConflictException("Schedule assignment conflict");
            }
        }
        assignment.setStatus(AssignmentStatus.ACTIVE);
        DepartmentScheduleAssignment saved = departmentAssignments.save(assignment);
        assignment.getDepartment().setSchedule(assignment.getSchedule());
        departments.save(assignment.getDepartment());
        attendanceQrs.synchronize(saved);
        return response(saved);
    }

    @Transactional(readOnly = true)
    public List<DepartmentScheduleAssignmentResponse> departmentAssignments(UUID organizationId) {
        ensureOrganization(organizationId);
        actor.requirePermission(Permission.ATTENDANCE_VIEW);

        return departmentAssignments.findAllByOrganizationId(organizationId).stream()
                .map(assignment -> new DepartmentScheduleAssignmentResponse(
                        assignment.getId(), assignment.getDepartment().getId(), assignment.getSchedule().getId(),
                        assignment.getEffectiveFrom(), assignment.getEffectiveTo(),
                        assignment.getStatus()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ScheduleAssignmentResponse> assignments(UUID organizationId) {
        ensureOrganization(organizationId);
        actor.requirePermission(Permission.ATTENDANCE_VIEW);

        return employeeAssignments.findAll().stream()
                .filter(assignment -> assignment.getOrganization().getId().equals(organizationId))
                .map(this::to)
                .toList();
    }

    private boolean overlap(LocalDate fromA, LocalDate toA, LocalDate fromB, LocalDate toB) {
        return (toA == null || !toA.isBefore(fromB))
                && (toB == null || !toB.isBefore(fromA));
    }

    public WorkSchedule scheduleFor(Employee employee, LocalDate date) {
        WorkSchedule employeeSchedule = employeeAssignments
                .findAllByOrganizationIdAndEmployeeId(employee.getOrganization().getId(), employee.getId()).stream()
                .filter(assignment -> assignment.getStatus() == AssignmentStatus.ACTIVE)
                .filter(assignment -> appliesOn(assignment.getEffectiveFrom(), assignment.getEffectiveTo(), date))
                .max(java.util.Comparator.comparing(EmployeeWorkSchedule::getEffectiveFrom))
                .map(EmployeeWorkSchedule::getWorkSchedule)
                .orElse(null);
        if (employeeSchedule != null) {
            return employeeSchedule;
        }

        if (employee.getDepartment() != null) {
            WorkSchedule departmentSchedule = departmentAssignments
                    .findAllByOrganizationIdAndDepartmentId(employee.getOrganization().getId(), employee.getDepartment().getId())
                    .stream()
                    .filter(assignment -> assignment.getStatus() == AssignmentStatus.ACTIVE)
                    .filter(assignment -> appliesOn(assignment.getEffectiveFrom(), assignment.getEffectiveTo(), date))
                    .max(java.util.Comparator.comparing(DepartmentScheduleAssignment::getEffectiveFrom))
                    .map(DepartmentScheduleAssignment::getSchedule)
                    .orElse(null);
            if (departmentSchedule != null) {
                return departmentSchedule;
            }
        }

        if (employee.getDepartment() != null && employee.getDepartment().getSchedule() != null) {
            return employee.getDepartment().getSchedule();
        }

        return schedules.findFirstByOrganizationIdAndOrganizationDefaultTrueAndStatus(
                employee.getOrganization().getId(),
                WorkScheduleStatus.ACTIVE
        ).orElse(null);
    }

    private boolean appliesOn(LocalDate effectiveFrom, LocalDate effectiveTo, LocalDate date) {
        return !effectiveFrom.isAfter(date) && (effectiveTo == null || !effectiveTo.isBefore(date));
    }

    private WorkSchedule schedule(UUID organizationId, UUID id) {
        return schedules.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new NotFoundException("Schedule not found"));
    }

    private WorkScheduleResponse response(WorkSchedule schedule) {
        return new WorkScheduleResponse(
                schedule.getId(),
                schedule.getOrganization().getId(),
                schedule.getName(),
                schedule.getDescription(),
                schedule.getTimezone(),
                schedule.getStatus(),
                schedule.getGracePeriodMinutes(),
                schedule.isOrganizationDefault(),
                schedule.getWorkdays(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt()
        );
    }

    private ScheduleAssignmentResponse to(EmployeeWorkSchedule assignment) {
        return new ScheduleAssignmentResponse(
                assignment.getId(),
                assignment.getEmployee().getId(),
                assignment.getWorkSchedule().getId(),
                assignment.getEffectiveFrom(),
                assignment.getEffectiveTo(),
                assignment.getStatus()
        );
    }

    private DepartmentScheduleAssignmentResponse response(DepartmentScheduleAssignment assignment) {
        return new DepartmentScheduleAssignmentResponse(
                assignment.getId(), assignment.getDepartment().getId(), assignment.getSchedule().getId(),
                assignment.getEffectiveFrom(), assignment.getEffectiveTo(),
                assignment.getStatus());
    }
}
