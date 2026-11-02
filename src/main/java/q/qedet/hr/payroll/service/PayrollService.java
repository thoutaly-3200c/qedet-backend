package q.qedet.hr.payroll.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.*;
import q.qedet.common.model.Permission;
import q.qedet.employee.entity.Employee;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.hr.common.event.*;
import q.qedet.hr.payroll.entity.*;
import q.qedet.hr.payroll.model.*;
import q.qedet.hr.payroll.repository.*;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.repository.OrganizationRepository;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
public class PayrollService {
    private final PayrollPeriodRepository periods;
    private final PayrollItemRepository items;
    private final EmployeeRepository employees;
    private final OrganizationRepository organizations;
    private final ActorContext actor;
    private final DomainEventPublisher publisher;

    public PayrollService(PayrollPeriodRepository periods, PayrollItemRepository items, EmployeeRepository employees,
                          OrganizationRepository organizations, ActorContext actor, DomainEventPublisher publisher) {
        this.periods = periods; this.items = items; this.employees = employees; this.organizations = organizations;
        this.actor = actor; this.publisher = publisher;
    }

    @Transactional
    public PayrollPeriod createPeriod(LocalDate startDate, LocalDate endDate) {
        actor.requirePermission(Permission.PAYROLL_MANAGE);
        if (startDate == null || endDate == null || endDate.isBefore(startDate))
            throw new BadRequestException("Invalid payroll period");
        Organization organization = organizations.findById(actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Organization not found"));
        PayrollPeriod period = new PayrollPeriod();
        period.setOrganization(organization); period.setStartDate(startDate); period.setEndDate(endDate);
        return periods.save(period);
    }

    @Transactional
    public PayrollPeriod open(UUID id) {
        actor.requirePermission(Permission.PAYROLL_MANAGE);
        PayrollPeriod period = ownedPeriod(id);
        if (period.isFinalized() || period.isOpen()) throw new ConflictException("Payroll period cannot be opened");
        period.setOpen(true);
        PayrollPeriod saved = periods.save(period);
        publish(null, saved.getOrganization().getId(), saved.getId(), PayrollEventType.PAYROLL_PERIOD_OPENED);
        return saved;
    }

    @Transactional
    public PayrollPeriod close(UUID id) {
        actor.requirePermission(Permission.PAYROLL_MANAGE);
        PayrollPeriod period = ownedPeriod(id);
        if (!period.isOpen() || period.isFinalized()) throw new ConflictException("Payroll period cannot be closed");
        period.setOpen(false);
        return periods.save(period);
    }

    @Transactional
    public PayrollPeriod finalizePeriod(UUID id) {
        actor.requirePermission(Permission.PAYROLL_MANAGE);
        PayrollPeriod period = ownedPeriod(id);
        if (period.isOpen() || period.isFinalized()) throw new ConflictException("Payroll period cannot be finalized");
        period.setFinalized(true);
        PayrollPeriod saved = periods.save(period);
        publish(null, saved.getOrganization().getId(), saved.getId(), PayrollEventType.PAYROLL_FINALIZED);
        return saved;
    }

    @Transactional
    public PayrollItem createItem(UUID periodId, UUID employeeId, PayrollItemType type,
                                  PayrollItemDirection direction, BigDecimal amount, LocalDate effectiveDate,
                                  PayrollItemSourceType sourceType, UUID sourceId, String description) {
        actor.requirePermission(Permission.PAYROLL_MANAGE);
        PayrollPeriod period = ownedPeriod(periodId);
        if (!period.isOpen() || period.isFinalized()) throw new ConflictException("Payroll period is not open");
        if (amount == null || amount.signum() <= 0) throw new BadRequestException("Payroll amount must be positive");
        PayrollItemSourceType resolvedSource = sourceType == null ? PayrollItemSourceType.MANUAL : sourceType;
        if (resolvedSource != PayrollItemSourceType.MANUAL && sourceId == null)
            throw new BadRequestException("Source id is required for non-manual payroll items");
        Employee employee = employees.findByIdAndOrganizationId(employeeId, actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Employee not found"));
        PayrollItem item = new PayrollItem();
        item.setOrganization(period.getOrganization()); item.setEmployee(employee); item.setPayrollPeriod(period);
        item.setType(type); item.setDirection(direction); item.setAmount(amount); item.setEffectiveDate(effectiveDate);
        item.setSourceType(resolvedSource); item.setSourceId(sourceId); item.setDescription(description);
        item.setCreatedBy(actor.employeeId());
        PayrollItem saved = items.save(item);
        publish(saved.getEmployee().getId(), saved.getOrganization().getId(), saved.getId(), PayrollEventType.PAYROLL_ITEM_CREATED);
        return saved;
    }

    @Transactional
    public PayrollItem adjustItem(UUID id, BigDecimal amount, String description) {
        actor.requirePermission(Permission.PAYROLL_MANAGE);
        PayrollItem item = ownedItem(id);
        ensureEditable(item);
        if (amount == null || amount.signum() <= 0) throw new BadRequestException("Payroll amount must be positive");
        item.setAmount(amount); item.setDescription(description); item.setStatus(PayrollItemStatus.PENDING);
        PayrollItem saved = items.save(item);
        publish(saved.getEmployee().getId(), saved.getOrganization().getId(), saved.getId(), PayrollEventType.PAYROLL_ITEM_ADJUSTED);
        return saved;
    }

    @Transactional
    public PayrollItem reverseItem(UUID id) {
        actor.requirePermission(Permission.PAYROLL_MANAGE);
        PayrollItem item = ownedItem(id);
        if (item.getStatus() == PayrollItemStatus.REVERSED) throw new ConflictException("Payroll item is already reversed");
        if (item.getPayrollPeriod().isFinalized()) throw new ConflictException("Finalized payroll items cannot be reversed");
        item.setStatus(PayrollItemStatus.REVERSED);
        PayrollItem saved = items.save(item);
        publish(saved.getEmployee().getId(), saved.getOrganization().getId(), saved.getId(), PayrollEventType.PAYROLL_ITEM_REVERSED);
        return saved;
    }

    private PayrollPeriod ownedPeriod(UUID id) {
        return periods.findByIdAndOrganizationId(id, actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Payroll period not found"));
    }
    private PayrollItem ownedItem(UUID id) {
        PayrollItem item = items.findById(id).orElseThrow(() -> new NotFoundException("Payroll item not found"));
        if (!item.getOrganization().getId().equals(actor.organizationId())) throw new NotFoundException("Payroll item not found");
        return item;
    }
    private void ensureEditable(PayrollItem item) {
        if (item.getPayrollPeriod().isFinalized() || item.getStatus() == PayrollItemStatus.REVERSED)
            throw new ConflictException("Payroll item cannot be changed");
    }
    private void publish(UUID employeeId, UUID organizationId, UUID aggregateId, PayrollEventType type) {
        publisher.publish(new DomainEvent(type.name(), organizationId, employeeId, aggregateId, Instant.now(), Map.of()));
    }
}
