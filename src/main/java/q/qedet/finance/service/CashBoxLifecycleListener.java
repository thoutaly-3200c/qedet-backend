package q.qedet.finance.service;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.hr.common.event.DomainEvent;
import q.qedet.hr.common.model.EmployeeLifecycleEventType;

@Component
class CashBoxLifecycleListener {
    private final EmployeeRepository employees;
    private final CashBoxProvisioningService cashBoxProvisioning;

    CashBoxLifecycleListener(EmployeeRepository employees, CashBoxProvisioningService cashBoxProvisioning) {
        this.employees = employees;
        this.cashBoxProvisioning = cashBoxProvisioning;
    }

    @EventListener
    public void onEmployeeLifecycleEvent(DomainEvent event) {
        if (!EmployeeLifecycleEventType.EMPLOYEE_ACTIVATED.name().equals(event.eventType())) {
            return;
        }
        employees.findByIdAndOrganizationId(event.employeeId(), event.organizationId())
                .ifPresent(cashBoxProvisioning::ensureEmployeeCashBox);
    }
}
