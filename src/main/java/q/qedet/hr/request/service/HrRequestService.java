package q.qedet.hr.request.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.*;
import q.qedet.common.model.Permission;
import q.qedet.employee.entity.Employee;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.hr.common.event.*;
import q.qedet.hr.request.entity.HrRequest;
import q.qedet.hr.request.model.*;
import q.qedet.hr.request.repository.HrRequestRepository;
import java.time.Instant;
import java.util.*;

@Service
public class HrRequestService {
    private final HrRequestRepository requests;
    private final EmployeeRepository employees;
    private final ActorContext actor;
    private final DomainEventPublisher publisher;

    public HrRequestService(HrRequestRepository requests, EmployeeRepository employees, ActorContext actor, DomainEventPublisher publisher) {
        this.requests = requests; this.employees = employees; this.actor = actor; this.publisher = publisher;
    }

    @Transactional
    public HrRequest create(UUID employeeId, HrRequestType type, String details) {
        actor.requirePermission(Permission.HR_REQUEST_MANAGE);
        Employee employee = employees.findByIdAndOrganizationId(employeeId, actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Employee not found"));
        Employee requester = employees.findByIdAndOrganizationId(actor.employeeId(), actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Requesting employee not found"));
        HrRequest request = new HrRequest(); request.setOrganization(employee.getOrganization()); request.setEmployee(employee);
        request.setRequestedBy(requester); request.setType(type); request.setDetails(details);
        HrRequest saved = requests.save(request); publish(saved, HrRequestEventType.REQUEST_CREATED); return saved;
    }

    @Transactional
    public HrRequest submit(UUID id) { return transition(id, HrRequestStatus.SUBMITTED, HrRequestEventType.REQUEST_SUBMITTED); }
    @Transactional
    public HrRequest approve(UUID id) { return transition(id, HrRequestStatus.APPROVED, HrRequestEventType.REQUEST_APPROVED); }
    @Transactional
    public HrRequest reject(UUID id) { return transition(id, HrRequestStatus.REJECTED, HrRequestEventType.REQUEST_REJECTED); }
    @Transactional
    public HrRequest execute(UUID id) { return transition(id, HrRequestStatus.EXECUTED, HrRequestEventType.REQUEST_EXECUTED); }

    private HrRequest transition(UUID id, HrRequestStatus target, HrRequestEventType eventType) {
        actor.requirePermission(Permission.HR_REQUEST_MANAGE);
        HrRequest request = requests.findByIdAndOrganizationId(id, actor.organizationId())
                .orElseThrow(() -> new NotFoundException("HR request not found"));
        boolean valid = switch (target) {
            case SUBMITTED -> request.getStatus() == HrRequestStatus.DRAFT;
            case APPROVED, REJECTED -> request.getStatus() == HrRequestStatus.SUBMITTED;
            case EXECUTED -> request.getStatus() == HrRequestStatus.APPROVED;
            default -> false;
        };
        if (!valid) throw new ConflictException("Invalid HR request transition");
        request.setStatus(target); HrRequest saved = requests.save(request); publish(saved, eventType); return saved;
    }

    private void publish(HrRequest request, HrRequestEventType type) {
        publisher.publish(new DomainEvent(type.name(), request.getOrganization().getId(),
                request.getEmployee().getId(), request.getId(), Instant.now(), Map.of("requestType", request.getType().name())));
    }
}
