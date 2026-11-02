package q.qedet.hr.onboarding.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.*;
import q.qedet.common.model.Permission;
import q.qedet.employee.entity.Employee;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.hr.common.event.DomainEventPublisher;
import q.qedet.hr.common.event.DomainEvent;
import q.qedet.hr.onboarding.dto.OnboardingTaskRequest;
import q.qedet.hr.onboarding.entity.*;
import q.qedet.hr.onboarding.model.*;
import q.qedet.hr.onboarding.repository.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class OnboardingService {
    private final OnboardingRepository onboardings;
    private final OnboardingTaskRepository tasks;
    private final EmployeeRepository employees;
    private final ActorContext actor;
    private final DomainEventPublisher publisher;

    public OnboardingService(OnboardingRepository onboardings, OnboardingTaskRepository tasks,
                             EmployeeRepository employees, ActorContext actor, DomainEventPublisher publisher) {
        this.onboardings = onboardings; this.tasks = tasks; this.employees = employees;
        this.actor = actor; this.publisher = publisher;
    }

    @Transactional
    public Onboarding create(UUID employeeId) {
        actor.requirePermission(Permission.EMPLOYEE_UPDATE);
        Employee employee = employees.findByIdAndOrganizationId(employeeId, actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Employee not found"));
        if (onboardings.findByEmployeeIdAndOrganizationId(employeeId, actor.organizationId()).isPresent())
            throw new ConflictException("Onboarding already exists");
        Onboarding value = new Onboarding();
        value.setOrganization(employee.getOrganization()); value.setEmployee(employee);
        value.setStatus(OnboardingStatus.IN_PROGRESS); value.setStartedAt(Instant.now());
        Onboarding saved = onboardings.save(value);
        publish(saved, OnboardingEventType.ONBOARDING_STARTED);
        return saved;
    }

    @Transactional
    public OnboardingTask addTask(UUID onboardingId, OnboardingTaskRequest request) {
        actor.requirePermission(Permission.EMPLOYEE_UPDATE);
        Onboarding onboarding = owned(onboardingId);
        OnboardingTask task = new OnboardingTask();
        task.setOrganization(onboarding.getOrganization()); task.setOnboarding(onboarding);
        task.setTitle(request.title().trim()); task.setDescription(request.description());
        if (request.assigneeEmployeeId() != null) {
            task.setAssignee(employees.findByIdAndOrganizationId(request.assigneeEmployeeId(), actor.organizationId())
                    .orElseThrow(() -> new NotFoundException("Assignee employee not found")));
        }
        return tasks.save(task);
    }

    @Transactional
    public OnboardingTask completeTask(UUID taskId) {
        actor.requirePermission(Permission.EMPLOYEE_UPDATE);
        OnboardingTask task = tasks.findByIdAndOrganizationId(taskId, actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Onboarding task not found"));
        if (task.getStatus() != OnboardingTaskStatus.PENDING) throw new ConflictException("Onboarding task is not pending");
        task.setStatus(OnboardingTaskStatus.COMPLETED); task.setCompletedAt(Instant.now());
        OnboardingTask saved = tasks.save(task);
        publish(saved.getOnboarding(), OnboardingEventType.ONBOARDING_TASK_COMPLETED);
        if (tasks.countByOnboardingIdAndStatus(task.getOnboarding().getId(), OnboardingTaskStatus.PENDING) == 0) {
            Onboarding onboarding = task.getOnboarding();
            onboarding.setStatus(OnboardingStatus.COMPLETED); onboarding.setCompletedAt(Instant.now());
            onboardings.save(onboarding); publish(onboarding, OnboardingEventType.ONBOARDING_COMPLETED);
        }
        return saved;
    }

    private Onboarding owned(UUID id) {
        return onboardings.findByIdAndOrganizationId(id, actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Onboarding not found"));
    }
    private void publish(Onboarding onboarding, OnboardingEventType type) {
        publisher.publish(new DomainEvent(type.name(), onboarding.getOrganization().getId(),
                onboarding.getEmployee().getId(), onboarding.getId(), Instant.now(), Map.of()));
    }
}
