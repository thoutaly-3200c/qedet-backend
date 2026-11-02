package q.qedet.finance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import q.qedet.common.model.BusinessType;
import q.qedet.common.model.EmployeeStatus;
import q.qedet.common.model.RoleType;
import q.qedet.employee.entity.Employee;
import q.qedet.finance.entity.CashBox;
import q.qedet.finance.model.CashBoxType;
import q.qedet.finance.repository.CashBoxRepository;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.entity.OrganizationConfiguration;
import q.qedet.organization.repository.OrganizationConfigurationRepository;
import q.qedet.organization.repository.OrganizationRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CashBoxProvisioningServiceTest {
    @Mock private OrganizationRepository organizations;
    @Mock private CashBoxRepository cashBoxes;
    @Mock private OrganizationConfigurationRepository configurations;

    private CashBoxProvisioningService service;
    private Organization organization;

    @BeforeEach
    void setUp() {
        service = new CashBoxProvisioningService(organizations, cashBoxes, configurations);
        organization = new Organization("Finance Test", BusinessType.GENERAL_TRADE);
        organization.setId(UUID.randomUUID());
    }

    @Test
    void createsOneZeroBalanceMainCashbox() {
        when(organizations.findByIdForUpdate(organization.getId())).thenReturn(Optional.of(organization));
        when(cashBoxes.findByOrganizationIdAndProvisioningKey(organization.getId(), "MAIN")).thenReturn(Optional.empty());
        when(configurations.findByOrganizationId(organization.getId())).thenReturn(Optional.of(settings("USD")));
        when(cashBoxes.save(any(CashBox.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CashBox cashBox = service.ensureMainCashBox(organization);

        assertThat(cashBox.getType()).isEqualTo(CashBoxType.MAIN);
        assertThat(cashBox.getName()).isEqualTo("Main Cashbox");
        assertThat(cashBox.getCurrency()).isEqualTo("USD");
        assertThat(cashBox.getOwnerEmployee()).isNull();
        verify(cashBoxes).save(cashBox);
    }

    @Test
    void newCashboxUsesOrganizationConfiguredCurrency() {
        when(configurations.findByOrganizationId(organization.getId())).thenReturn(Optional.of(settings("SAR")));
        when(organizations.findByIdForUpdate(organization.getId())).thenReturn(Optional.of(organization));
        when(cashBoxes.findByOrganizationIdAndProvisioningKey(organization.getId(), "MAIN")).thenReturn(Optional.empty());
        when(cashBoxes.save(any(CashBox.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(service.ensureMainCashBox(organization).getCurrency()).isEqualTo("SAR");
    }

    @Test
    void returnsExistingMainCashboxWithoutCreatingDuplicate() {
        CashBox existing = new CashBox();
        existing.setCurrency("USD");
        when(configurations.findByOrganizationId(organization.getId())).thenReturn(Optional.of(settings("USD")));
        when(organizations.findByIdForUpdate(organization.getId())).thenReturn(Optional.of(organization));
        when(cashBoxes.findByOrganizationIdAndProvisioningKey(organization.getId(), "MAIN")).thenReturn(Optional.of(existing));

        assertThat(service.ensureMainCashBox(organization)).isSameAs(existing);

        verify(cashBoxes, never()).save(any());
    }

    @Test
    void createsOneCashboxForAnActiveFinanceEmployee() {
        Employee employee = employee(RoleType.FINANCE_EMPLOYEE, EmployeeStatus.ACTIVE);
        String key = "EMPLOYEE-" + employee.getId().toString().replace("-", "").toUpperCase();
        when(organizations.findByIdForUpdate(organization.getId())).thenReturn(Optional.of(organization));
        when(cashBoxes.findByOrganizationIdAndProvisioningKey(organization.getId(), key)).thenReturn(Optional.empty());
        when(configurations.findByOrganizationId(organization.getId())).thenReturn(Optional.of(settings("USD")));
        when(cashBoxes.save(any(CashBox.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CashBox cashBox = service.ensureEmployeeCashBox(employee);

        assertThat(cashBox.getType()).isEqualTo(CashBoxType.EMPLOYEE);
        assertThat(cashBox.getOwnerEmployee()).isSameAs(employee);
        assertThat(cashBox.getName()).isEqualTo("Cashbox - Finance User");
        assertThat(cashBox.getCurrency()).isEqualTo("USD");
    }

    @Test
    void ignoresInactiveAndNonFinanceEmployees() {
        assertThat(service.ensureEmployeeCashBox(employee(RoleType.FINANCE_EMPLOYEE, EmployeeStatus.INVITED))).isNull();
        assertThat(service.ensureEmployeeCashBox(employee(RoleType.EMPLOYEE, EmployeeStatus.ACTIVE))).isNull();

        verifyNoInteractions(cashBoxes);
    }

    private Employee employee(RoleType role, EmployeeStatus status) {
        Employee employee = new Employee(organization, "EMP-00001", "Finance", "User",
                "finance@example.com", null, role);
        employee.setId(UUID.randomUUID());
        employee.setStatus(status);
        return employee;
    }

    private OrganizationConfiguration settings(String currency) {
        OrganizationConfiguration settings = new OrganizationConfiguration();
        settings.setCurrency(currency);
        return settings;
    }
}
