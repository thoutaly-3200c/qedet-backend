package q.qedet.organization.policy.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import q.qedet.organization.policy.dto.DepartmentScheduleAssignmentRequest;
import q.qedet.organization.policy.dto.DepartmentScheduleAssignmentResponse;
import q.qedet.organization.policy.dto.ScheduleAssignmentRequest;
import q.qedet.organization.policy.dto.ScheduleAssignmentResponse;
import q.qedet.organization.policy.dto.WorkScheduleRequest;
import q.qedet.organization.policy.dto.WorkScheduleResponse;
import q.qedet.organization.policy.service.AttendanceScheduleService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/attendance")
public class AttendanceScheduleController {

    private final AttendanceScheduleService service;

    public AttendanceScheduleController(AttendanceScheduleService service) {
        this.service = service;
    }

    @PostMapping("/schedules")
    public WorkScheduleResponse create(
            @PathVariable UUID organizationId,
            @Valid @RequestBody WorkScheduleRequest request) {
        return service.create(organizationId, request);
    }

    @GetMapping("/schedules")
    public List<WorkScheduleResponse> list(@PathVariable UUID organizationId) {
        return service.list(organizationId);
    }

    @GetMapping("/schedules/{id}")
    public WorkScheduleResponse get(
            @PathVariable UUID organizationId,
            @PathVariable UUID id) {
        return service.get(organizationId, id);
    }

    @PutMapping("/schedules/{id}")
    public WorkScheduleResponse update(
            @PathVariable UUID organizationId,
            @PathVariable UUID id,
            @Valid @RequestBody WorkScheduleRequest request) {
        return service.update(organizationId, id, request);
    }

    @PostMapping("/schedules/{id}/activate")
    public WorkScheduleResponse activate(
            @PathVariable UUID organizationId,
            @PathVariable UUID id) {
        return service.activate(organizationId, id);
    }

    @PostMapping("/schedules/{id}/archive")
    public WorkScheduleResponse archive(
            @PathVariable UUID organizationId,
            @PathVariable UUID id) {
        return service.archive(organizationId, id);
    }

    @PostMapping("/schedules/{id}/restore")
    public WorkScheduleResponse restore(
            @PathVariable UUID organizationId,
            @PathVariable UUID id) {
        return service.restore(organizationId, id);
    }

    @PostMapping("/department-schedule-assignments")
    public DepartmentScheduleAssignmentResponse assignDepartment(
            @PathVariable UUID organizationId,
            @Valid @RequestBody DepartmentScheduleAssignmentRequest request) {
        return service.assignDepartment(organizationId, request);
    }

    @PutMapping("/department-schedule-assignments/{assignmentId}")
    public DepartmentScheduleAssignmentResponse updateDepartment(
            @PathVariable UUID organizationId,
            @PathVariable UUID assignmentId,
            @Valid @RequestBody DepartmentScheduleAssignmentRequest request) {
        return service.updateDepartmentAssignment(organizationId, assignmentId, request);
    }

    @PostMapping("/department-schedule-assignments/{assignmentId}/end")
    public DepartmentScheduleAssignmentResponse endDepartment(
            @PathVariable UUID organizationId,
            @PathVariable UUID assignmentId) {
        return service.endDepartmentAssignment(organizationId, assignmentId);
    }

    @PostMapping("/department-schedule-assignments/{assignmentId}/activate")
    public DepartmentScheduleAssignmentResponse activateDepartment(
            @PathVariable UUID organizationId,
            @PathVariable UUID assignmentId) {
        return service.activateDepartmentAssignment(organizationId, assignmentId);
    }

    @GetMapping("/department-schedule-assignments")
    public List<DepartmentScheduleAssignmentResponse> departments(@PathVariable UUID organizationId) {
        return service.departmentAssignments(organizationId);
    }

    @PostMapping("/schedule-assignments")
    public List<ScheduleAssignmentResponse> assign(
            @PathVariable UUID organizationId,
            @Valid @RequestBody ScheduleAssignmentRequest request) {
        return service.assignEmployees(organizationId, request);
    }

    @GetMapping("/schedule-assignments")
    public List<ScheduleAssignmentResponse> assignments(@PathVariable UUID organizationId) {
        return service.assignments(organizationId);
    }
}
