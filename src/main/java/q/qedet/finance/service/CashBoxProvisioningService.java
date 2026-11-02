
package q.qedet.finance.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.common.model.EmployeeStatus;
import q.qedet.common.model.RoleType;
import q.qedet.employee.entity.Employee;
import q.qedet.finance.entity.CashBox;
import q.qedet.finance.model.CashBoxType;
import q.qedet.finance.repository.CashBoxRepository;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.repository.OrganizationRepository;
import q.qedet.organization.repository.OrganizationConfigurationRepository;

@Service
public class CashBoxProvisioningService {

    static final String MAIN_PROVISIONING_KEY = "MAIN";

    private final OrganizationRepository organizations;
    private final CashBoxRepository cashBoxes;
    private final OrganizationConfigurationRepository configurations;

    public CashBoxProvisioningService(
            OrganizationRepository organizations,
            CashBoxRepository cashBoxes,
            OrganizationConfigurationRepository configurations
    ) {
        this.organizations = organizations;
        this.cashBoxes = cashBoxes;
        this.configurations = configurations;
    }

    private String currency(Organization organization) {
        return configurations.findByOrganizationId(organization.getId())
                .map(c -> c.getCurrency())
                .filter(c -> c != null && !c.isBlank())
                .map(String::trim)
                .map(String::toUpperCase)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Organization currency is not configured: "
                                        + organization.getId()
                        )
                );
    }

    private CashBox validateCurrency(
            CashBox cashBox,
            Organization organization
    ) {
        String expectedCurrency = currency(organization);

        if (!expectedCurrency.equalsIgnoreCase(cashBox.getCurrency())) {
            throw new IllegalStateException(
                    "Cashbox currency mismatch. Expected "
                            + expectedCurrency
                            + " but found "
                            + cashBox.getCurrency()
                            + " for cashbox "
                            + cashBox.getId()
            );
        }

        return cashBox;
    }

    @Transactional
    public CashBox ensureMainCashBox(Organization organization) {
        Organization lockedOrganization = organizations
                .findByIdForUpdate(organization.getId())
                .orElseThrow();

        return cashBoxes.findByOrganizationIdAndProvisioningKey(
                        lockedOrganization.getId(),
                        MAIN_PROVISIONING_KEY
                )
                .map(cashBox -> validateCurrency(cashBox, lockedOrganization))
                .orElseGet(() -> createMain(lockedOrganization));
    }

    @Transactional
    public CashBox ensureCashierCashBox(Employee employee) {
        if (employee == null
                || employee.getStatus() != EmployeeStatus.ACTIVE
                || employee.getRole() != RoleType.CASHIER) {
            return null;
        }

        Organization organization = organizations
                .findByIdForUpdate(employee.getOrganization().getId())
                .orElseThrow();

        String provisioningKey = employeeProvisioningKey(employee);

        return cashBoxes.findByOrganizationIdAndProvisioningKey(
                        organization.getId(),
                        provisioningKey
                )
                .map(cashBox -> validateCurrency(cashBox, organization))
                .orElseGet(() -> createEmployee(
                        organization,
                        employee,
                        provisioningKey
                ));
    }

    @Transactional
    public CashBox ensureEmployeeCashBox(Employee employee) {
        if (employee == null
                || employee.getStatus() != EmployeeStatus.ACTIVE
                || !isFinanceRole(employee.getRole())) {
            return null;
        }

        Organization organization = organizations
                .findByIdForUpdate(employee.getOrganization().getId())
                .orElseThrow();

        String provisioningKey = employeeProvisioningKey(employee);

        return cashBoxes.findByOrganizationIdAndProvisioningKey(
                        organization.getId(),
                        provisioningKey
                )
                .map(cashBox -> validateCurrency(cashBox, organization))
                .orElseGet(() -> createEmployee(
                        organization,
                        employee,
                        provisioningKey
                ));
    }

    private CashBox createMain(Organization organization) {
        CashBox cashBox = new CashBox();
        cashBox.setOrganization(organization);
        cashBox.setName("Main Cashbox");
        cashBox.setCurrency(currency(organization));
        cashBox.setType(CashBoxType.MAIN);
        cashBox.setProvisioningKey(MAIN_PROVISIONING_KEY);

        return cashBoxes.save(cashBox);
    }

    private CashBox createEmployee(
            Organization organization,
            Employee employee,
            String provisioningKey
    ) {
        CashBox cashBox = new CashBox();
        cashBox.setOrganization(organization);
        cashBox.setOwnerEmployee(employee);

        cashBox.setName(
                "Cashbox - "
                        + employee.getFirstName()
                        + " "
                        + employee.getLastName()
        );

        cashBox.setCurrency(currency(organization));
        cashBox.setType(CashBoxType.EMPLOYEE);
        cashBox.setProvisioningKey(provisioningKey);

        return cashBoxes.save(cashBox);
    }

    private boolean isFinanceRole(RoleType role) {
        return role == RoleType.FINANCE_MANAGER
                || role == RoleType.FINANCE_EMPLOYEE;
    }

    private String employeeProvisioningKey(Employee employee) {
        return "EMPLOYEE-" +
                employee.getId().toString()
                        .replace("-", "")
                        .toUpperCase();
    }
}
