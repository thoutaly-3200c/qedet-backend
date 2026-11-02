package q.qedet.finance.service;

import org.junit.jupiter.api.Test;
import q.qedet.common.model.BusinessType;
import q.qedet.common.model.RoleType;
import q.qedet.employee.entity.Employee;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.hr.common.event.DomainEvent;
import q.qedet.hr.common.model.EmployeeLifecycleEventType;
import q.qedet.organization.entity.Organization;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

class CashBoxLifecycleListenerTest {
    @Test
    void provisionsCashboxOnlyForEmployeeActivationEvents() {
        EmployeeRepository employees = mock(EmployeeRepository.class);
        CashBoxProvisioningService provisioning = mock(CashBoxProvisioningService.class);
        CashBoxLifecycleListener listener = new CashBoxLifecycleListener(employees, provisioning);
        UUID organizationId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        Organization organization = new Organization("Lifecycle Test", BusinessType.GENERAL_TRADE);
        organization.setId(organizationId);
        Employee employee = new Employee(organization, "EMP-00001", "Finance", "Employee",
                "finance@example.com", null, RoleType.FINANCE_EMPLOYEE);
        employee.setId(employeeId);
        when(employees.findByIdAndOrganizationId(employeeId, organizationId)).thenReturn(Optional.of(employee));

        listener.onEmployeeLifecycleEvent(new DomainEvent(EmployeeLifecycleEventType.EMPLOYEE_ACTIVATED.name(),
                organizationId, employeeId, employeeId, Instant.now(), Map.of()));
        listener.onEmployeeLifecycleEvent(new DomainEvent(EmployeeLifecycleEventType.EMPLOYEE_TERMINATED.name(),
                organizationId, employeeId, employeeId, Instant.now(), Map.of()));

        verify(provisioning).ensureEmployeeCashBox(employee);
        verifyNoMoreInteractions(provisioning);
    }
}
