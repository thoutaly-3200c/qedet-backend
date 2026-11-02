
package q.qedet.sales.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import q.qedet.common.exception.ConflictException;
import q.qedet.common.model.LocationStatus;
import q.qedet.common.model.LocationType;
import q.qedet.common.model.RoleType;
import q.qedet.employee.entity.Employee;
import q.qedet.organization.entity.OrganizationSite;
import q.qedet.organization.repository.OrganizationSiteRepository;
import q.qedet.sales.entity.PosAssignment;
import q.qedet.sales.repository.PosAssignmentRepository;

@Service
@RequiredArgsConstructor
public class PosAssignmentProvisioningService {

    private final PosAssignmentRepository assignments;
    private final OrganizationSiteRepository sites;

    @Transactional
    public void ensureCashierPos(Employee employee) {

        if (employee.getRole() != RoleType.CASHIER) {
            return;
        }

        var organizationId = employee.getOrganization().getId();

        // Preserve an existing assignment.
        if (assignments.findByOrganizationIdAndCashierId(
                organizationId, employee.getId()).isPresent()) {
            return;
        }

        OrganizationSite salesHall = sites
                .findByOrganizationIdAndTypeAndStatus(
                        organizationId,
                        LocationType.SALES_HALL,
                        LocationStatus.ACTIVE
                )
                .orElseThrow(() -> new ConflictException(
                        "POS_SALES_HALL_MISSING",
                        "No active Sales Hall configured for cashier POS"
                ));

        PosAssignment assignment = new PosAssignment();

        assignment.setOrganization(employee.getOrganization());
        assignment.setCashier(employee);
        assignment.setSalesHall(salesHall);

        // Generate a unique POS code for this cashier.
        assignment.setCode("POS-" +
                employee.getId().toString()
                        .substring(0, 8).toUpperCase());

        assignment.setActive(true);

        assignments.save(assignment);
    }
}
