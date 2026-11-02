package q.qedet.hr.compensation.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.*;
import q.qedet.common.model.Permission;
import q.qedet.employee.entity.Employee;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.hr.common.event.*;
import q.qedet.hr.compensation.entity.*;
import q.qedet.hr.compensation.model.CompensationEventType;
import q.qedet.hr.compensation.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
public class CompensationService {
    private final EmployeeCompensationRepository compensations;
    private final CompensationHistoryRepository history;
    private final CompensationAllowanceRepository allowances;
    private final EmployeeBenefitRepository benefits;
    private final EmployeeRepository employees;
    private final ActorContext actor;
    private final DomainEventPublisher publisher;

    public CompensationService(EmployeeCompensationRepository compensations, CompensationHistoryRepository history,
                               CompensationAllowanceRepository allowances, EmployeeBenefitRepository benefits,
                               EmployeeRepository employees, ActorContext actor, DomainEventPublisher publisher) {
        this.compensations = compensations; this.history = history; this.allowances = allowances;
        this.benefits = benefits; this.employees = employees; this.actor = actor; this.publisher = publisher;
    }

    @Transactional
    public EmployeeCompensation setSalary(UUID employeeId, BigDecimal salary, BigDecimal overtimeRate,
                                          LocalDate effectiveFrom, String reason) {
        actor.requirePermission(Permission.COMPENSATION_MANAGE);
        if (salary == null || salary.signum() < 0 || overtimeRate == null || overtimeRate.signum() < 0)
            throw new BadRequestException("Compensation amounts cannot be negative");
        Employee employee = employee(employeeId);
        Optional<EmployeeCompensation> existing = compensations.findByEmployeeIdAndOrganizationId(employeeId, actor.organizationId());
        EmployeeCompensation value = existing.orElseGet(EmployeeCompensation::new);
        value.setOrganization(employee.getOrganization()); value.setEmployee(employee);
        value.setBaseSalary(salary); value.setOvertimeRate(overtimeRate);
        if (value.getRecurringAllowance() == null) value.setRecurringAllowance(BigDecimal.ZERO);
        value.setEffectiveFrom(effectiveFrom == null ? LocalDate.now() : effectiveFrom);
        EmployeeCompensation saved = compensations.save(value);
        CompensationHistory snapshot = new CompensationHistory();
        snapshot.setOrganization(saved.getOrganization()); snapshot.setEmployee(employee);
        snapshot.setBaseSalary(saved.getBaseSalary()); snapshot.setRecurringAllowance(saved.getRecurringAllowance());
        snapshot.setOvertimeRate(saved.getOvertimeRate()); snapshot.setEffectiveFrom(saved.getEffectiveFrom()); snapshot.setReason(reason);
        history.save(snapshot);
        publish(employee, saved.getId(), existing.isEmpty() ? CompensationEventType.COMPENSATION_CREATED : CompensationEventType.SALARY_CHANGED);
        return saved;
    }

    @Transactional
    public CompensationAllowance addAllowance(UUID employeeId, String name, BigDecimal amount, LocalDate effectiveFrom) {
        actor.requirePermission(Permission.COMPENSATION_MANAGE);
        if (amount == null || amount.signum() <= 0) throw new BadRequestException("Allowance must be positive");
        Employee employee = employee(employeeId);
        CompensationAllowance allowance = new CompensationAllowance();
        allowance.setOrganization(employee.getOrganization()); allowance.setEmployee(employee); allowance.setName(name.trim());
        allowance.setAmount(amount); allowance.setEffectiveFrom(effectiveFrom == null ? LocalDate.now() : effectiveFrom);
        CompensationAllowance saved = allowances.save(allowance);
        publish(employee, saved.getId(), CompensationEventType.ALLOWANCE_ADDED);
        return saved;
    }

    @Transactional
    public CompensationAllowance removeAllowance(UUID id) {
        actor.requirePermission(Permission.COMPENSATION_MANAGE);
        CompensationAllowance allowance = allowances.findByIdAndOrganizationId(id, actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Allowance not found"));
        allowance.setActive(false); allowance.setEffectiveTo(LocalDate.now());
        CompensationAllowance saved = allowances.save(allowance);
        publish(saved.getEmployee(), saved.getId(), CompensationEventType.ALLOWANCE_REMOVED);
        return saved;
    }

    @Transactional
    public EmployeeBenefit assignBenefit(UUID employeeId, String benefitCode, String description, LocalDate effectiveFrom) {
        actor.requirePermission(Permission.COMPENSATION_MANAGE);
        Employee employee = employee(employeeId);
        EmployeeBenefit benefit = new EmployeeBenefit();
        benefit.setOrganization(employee.getOrganization()); benefit.setEmployee(employee);
        benefit.setBenefitCode(benefitCode.trim()); benefit.setDescription(description);
        benefit.setEffectiveFrom(effectiveFrom == null ? LocalDate.now() : effectiveFrom);
        EmployeeBenefit saved = benefits.save(benefit);
        publish(employee, saved.getId(), CompensationEventType.BENEFIT_ASSIGNED);
        return saved;
    }

    @Transactional
    public EmployeeBenefit removeBenefit(UUID id) {
        actor.requirePermission(Permission.COMPENSATION_MANAGE);
        EmployeeBenefit benefit = benefits.findByIdAndOrganizationId(id, actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Benefit not found"));
        benefit.setActive(false); benefit.setEffectiveTo(LocalDate.now());
        return benefits.save(benefit);
    }

    private Employee employee(UUID id) {
        return employees.findByIdAndOrganizationId(id, actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Employee not found"));
    }
    private void publish(Employee employee, UUID aggregateId, CompensationEventType type) {
        publisher.publish(new DomainEvent(type.name(), employee.getOrganization().getId(), employee.getId(),
                aggregateId, Instant.now(), Map.of()));
    }
}
