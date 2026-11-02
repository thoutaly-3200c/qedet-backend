package q.qedet.hr.leave.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.*;
import q.qedet.common.model.Permission;
import q.qedet.employee.entity.Employee;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.hr.attendance.entity.AttendanceRecord;
import q.qedet.hr.attendance.model.AttendanceStatus;
import q.qedet.hr.attendance.repository.AttendanceRecordRepository;
import q.qedet.hr.common.event.*;
import q.qedet.hr.leave.entity.*;
import q.qedet.hr.leave.model.*;
import q.qedet.hr.leave.repository.*;
import java.time.*;
import java.util.Map;
import java.util.UUID;

@Service
public class LeaveService {
    private final LeaveRequestRepository requests;
    private final LeaveTypeRepository types;
    private final EmployeeRepository employees;
    private final AttendanceRecordRepository attendance;
    private final ActorContext actor;
    private final DomainEventPublisher publisher;

    public LeaveService(LeaveRequestRepository requests, LeaveTypeRepository types, EmployeeRepository employees,
                        AttendanceRecordRepository attendance, ActorContext actor, DomainEventPublisher publisher) {
        this.requests = requests; this.types = types; this.employees = employees; this.attendance = attendance;
        this.actor = actor; this.publisher = publisher;
    }

    @Transactional
    public LeaveRequest create(UUID employeeId, UUID leaveTypeId, LocalDate start, LocalDate end, String reason) {
        actor.requirePermission(Permission.LEAVE_MANAGE);
        Employee employee = employees.findByIdAndOrganizationId(employeeId, actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Employee not found"));
        LeaveType type = types.findByIdAndOrganizationId(leaveTypeId, actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Leave type not found"));
        if (start == null || end == null || end.isBefore(start)) throw new BadRequestException("Invalid leave dates");
        LeaveRequest request = new LeaveRequest();
        request.setOrganization(employee.getOrganization()); request.setEmployee(employee); request.setLeaveType(type);
        request.setStartDate(start); request.setEndDate(end); request.setReason(reason);
        LeaveRequest saved = requests.save(request);
        publish(saved, LeaveEventType.LEAVE_REQUESTED);
        return saved;
    }

    @Transactional
    public LeaveRequest approve(UUID id) {
        actor.requirePermission(Permission.LEAVE_MANAGE);
        LeaveRequest request = owned(id);
        if (request.getStatus() != LeaveRequestStatus.REQUESTED) throw new ConflictException("Leave request is not pending");
        request.setStatus(LeaveRequestStatus.APPROVED);
        LeaveRequest saved = requests.save(request);
        for (LocalDate date = saved.getStartDate(); !date.isAfter(saved.getEndDate()); date = date.plusDays(1)) {
            LocalDate currentDate = date;
            AttendanceRecord record = attendance.findByEmployeeIdAndWorkDate(saved.getEmployee().getId(), currentDate)
                    .orElseGet(() -> attendance.save(new AttendanceRecord(saved.getOrganization(), saved.getEmployee(), currentDate)));
            record.setStatus(AttendanceStatus.ON_LEAVE); record.setAuthorizedLeave(true); attendance.save(record);
        }
        publish(saved, LeaveEventType.LEAVE_APPROVED);
        return saved;
    }

    @Transactional
    public LeaveRequest reject(UUID id) {
        actor.requirePermission(Permission.LEAVE_MANAGE);
        LeaveRequest request = owned(id);
        if (request.getStatus() != LeaveRequestStatus.REQUESTED) throw new ConflictException("Leave request is not pending");
        request.setStatus(LeaveRequestStatus.REJECTED);
        LeaveRequest saved = requests.save(request);
        publish(saved, LeaveEventType.LEAVE_REJECTED);
        return saved;
    }

    private LeaveRequest owned(UUID id) {
        return requests.findByIdAndOrganizationId(id, actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Leave request not found"));
    }
    private void publish(LeaveRequest request, LeaveEventType type) {
        publisher.publish(new DomainEvent(type.name(), request.getOrganization().getId(), request.getEmployee().getId(),
                request.getId(), Instant.now(), Map.of("leaveRequestId", request.getId().toString())));
    }
}
