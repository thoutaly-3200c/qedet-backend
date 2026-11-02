package q.qedet.hr.performance.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.*;
import q.qedet.common.model.Permission;
import q.qedet.employee.entity.Employee;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.hr.common.event.*;
import q.qedet.hr.performance.entity.*;
import q.qedet.hr.performance.model.*;
import q.qedet.hr.performance.repository.*;
import java.time.Instant;
import java.util.*;

@Service
public class PerformanceService {
    private final GoalRepository goals;
    private final PerformanceReviewRepository reviews;
    private final EmployeeRepository employees;
    private final ActorContext actor;
    private final DomainEventPublisher publisher;

    public PerformanceService(GoalRepository goals, PerformanceReviewRepository reviews, EmployeeRepository employees,
                              ActorContext actor, DomainEventPublisher publisher) {
        this.goals = goals; this.reviews = reviews; this.employees = employees; this.actor = actor; this.publisher = publisher;
    }

    @Transactional
    public Goal createGoal(UUID employeeId, String title, String description, java.time.LocalDate dueDate) {
        actor.requirePermission(Permission.PERFORMANCE_MANAGE);
        Employee employee = employees.findByIdAndOrganizationId(employeeId, actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Employee not found"));
        Goal goal = new Goal(); goal.setOrganization(employee.getOrganization()); goal.setEmployee(employee);
        goal.setTitle(title.trim()); goal.setDescription(description); goal.setDueDate(dueDate);
        Goal saved = goals.save(goal);
        publish(saved.getOrganization().getId(), saved.getEmployee().getId(), saved.getId(), PerformanceEventType.GOAL_CREATED);
        return saved;
    }

    @Transactional
    public Goal completeGoal(UUID id) {
        actor.requirePermission(Permission.PERFORMANCE_MANAGE);
        Goal goal = goals.findByIdAndOrganizationId(id, actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Goal not found"));
        if (goal.getStatus() != GoalStatus.ACTIVE) throw new ConflictException("Goal is not active");
        goal.setStatus(GoalStatus.COMPLETED); goal.setCompletedAt(Instant.now());
        Goal saved = goals.save(goal);
        publish(saved.getOrganization().getId(), saved.getEmployee().getId(), saved.getId(), PerformanceEventType.GOAL_COMPLETED);
        return saved;
    }

    @Transactional
    public PerformanceReview submitReview(UUID id) {
        actor.requirePermission(Permission.PERFORMANCE_MANAGE);
        PerformanceReview review = reviews.findByIdAndOrganizationId(id, actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Performance review not found"));
        if (review.getStatus() != PerformanceReviewStatus.DRAFT) throw new ConflictException("Review is not a draft");
        review.setStatus(PerformanceReviewStatus.SUBMITTED);
        PerformanceReview saved = reviews.save(review);
        publish(saved.getOrganization().getId(), saved.getEmployee().getId(), saved.getId(), PerformanceEventType.REVIEW_SUBMITTED);
        return saved;
    }

    private void publish(UUID organizationId, UUID employeeId, UUID aggregateId, PerformanceEventType type) {
        publisher.publish(new DomainEvent(type.name(), organizationId, employeeId, aggregateId, Instant.now(), Map.of()));
    }
}
