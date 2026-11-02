package q.qedet.warehouse.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.ConflictException;
import q.qedet.common.model.LocationStatus;
import q.qedet.common.model.LocationType;
import q.qedet.employee.entity.Employee;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.entity.OrganizationSite;
import q.qedet.organization.repository.OrganizationSiteRepository;
import q.qedet.organization.repository.OrganizationZoneRepository;
import q.qedet.procurement.entity.PurchaseOrder;
import q.qedet.procurement.repository.PurchaseOrderItemRepository;
import q.qedet.procurement.repository.PurchaseOrderRepository;
import q.qedet.product.repository.OrganizationProductRepository;
import q.qedet.warehouse.dto.ReceivingDtos;
import q.qedet.warehouse.entity.ReceivingOperation;
import q.qedet.warehouse.entity.ReceivingRequest;
import q.qedet.warehouse.repository.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceivingWarehouseAssignmentTest {
    @Mock ActorContext actor;
    @Mock EmployeeRepository employees;
    @Mock PurchaseOrderRepository purchaseOrders;
    @Mock PurchaseOrderItemRepository purchaseOrderItems;
    @Mock ReceivingRequestRepository requests;
    @Mock ReceivingRequestItemRepository requestItems;
    @Mock ReceivingOperationRepository operations;
    @Mock ReceivingOperationItemRepository operationItems;
    @Mock ReceivingOperationSerialRepository serials;
    @Mock ReceivingOperationZoneAssignmentRepository assignments;
    @Mock OrganizationSiteRepository sites;
    @Mock OrganizationZoneRepository zones;
    @Mock OrganizationProductRepository organizationProducts;
    @Mock WarehouseService warehouseService;
    Organization organization;
    Employee employee;
    ReceivingRequest request;

    @BeforeEach void setUp() {
        organization = new Organization(); organization.setId(UUID.randomUUID());
        employee = new Employee(); employee.setId(UUID.randomUUID());
        PurchaseOrder order = new PurchaseOrder(); order.setId(UUID.randomUUID());
        order.setOrganization(organization); order.setNumber("PO-1");
        request = new ReceivingRequest(); request.setId(UUID.randomUUID());
        request.setOrganization(organization); request.setPurchaseOrder(order); request.setRequestNumber("RR-PO-1");
        when(actor.organizationId()).thenReturn(organization.getId());
        when(requests.findByIdAndOrganizationId(request.getId(), organization.getId())).thenReturn(Optional.of(request));
    }

    @Test void startSelectsOnlyActiveWarehouseSite() {
        OrganizationSite warehouse = site(LocationType.WAREHOUSE, true, false);
        when(sites.findAllByOrganizationIdAndStatusOrderByNameAsc(organization.getId(), LocationStatus.ACTIVE))
                .thenReturn(List.of(site(LocationType.SALES_HALL, true, false), warehouse));
        stubEmployee();
        service().start(request.getId(), new ReceivingDtos.StartOperationRequest("counted"));
        assertThat(request.getDestinationLocation()).isSameAs(warehouse);
        assertThat(request.getStatus()).isEqualTo(ReceivingRequest.Status.IN_PROGRESS);
        verify(operations).save(any(ReceivingOperation.class));
    }

    @Test void startRequiresSelectionWhenWarehouseSitesAreAmbiguous() {
        when(sites.findAllByOrganizationIdAndStatusOrderByNameAsc(organization.getId(), LocationStatus.ACTIVE))
                .thenReturn(List.of(site(LocationType.WAREHOUSE, true, false), site(LocationType.WAREHOUSE, true, false)));
        assertThatThrownBy(() -> service().start(request.getId(), null))
                .isInstanceOf(ConflictException.class).hasMessageContaining("Select an active Warehouse site");
        verify(operations, never()).save(any());
    }

    @Test void startUsesDefaultWarehouseAmongSeveralSites() {
        OrganizationSite selected = site(LocationType.WAREHOUSE, true, true);
        when(sites.findAllByOrganizationIdAndStatusOrderByNameAsc(organization.getId(), LocationStatus.ACTIVE))
                .thenReturn(List.of(site(LocationType.WAREHOUSE, true, false), selected));
        stubEmployee();
        service().start(request.getId(), null);
        assertThat(request.getDestinationLocation()).isSameAs(selected);
    }

    @Test void startRejectsInactiveExistingWarehouseSite() {
        OrganizationSite inactive = site(LocationType.WAREHOUSE, false, false);
        request.setDestinationLocation(inactive);
        when(sites.findByIdAndOrganizationId(inactive.getId(), organization.getId())).thenReturn(Optional.of(inactive));
        assertThatThrownBy(() -> service().start(request.getId(), null))
                .isInstanceOf(ConflictException.class).hasMessageContaining("active Warehouse site");
    }

    @Test void startRejectsWarehouseFromAnotherOrganization() {
        UUID foreignSiteId = UUID.randomUUID();
        assertThatThrownBy(() -> service().start(request.getId(),
                new ReceivingDtos.StartOperationRequest(null, foreignSiteId, null)))
                .isInstanceOf(ConflictException.class).hasMessageContaining("active Warehouse site");
        verify(sites).findByIdAndOrganizationId(foreignSiteId, organization.getId());
    }

    private void stubEmployee() {
        when(actor.employeeId()).thenReturn(employee.getId());
        when(employees.findByIdAndOrganizationId(employee.getId(), organization.getId())).thenReturn(Optional.of(employee));
        when(operations.save(any(ReceivingOperation.class))).thenAnswer(invocation -> {
            ReceivingOperation operation = invocation.getArgument(0); operation.setId(UUID.randomUUID()); return operation;
        });
    }
    private OrganizationSite site(LocationType type, boolean active, boolean selected) {
        OrganizationSite site = new OrganizationSite(); site.setId(UUID.randomUUID());
        site.setOrganization(organization); site.setType(type);
        site.setStatus(active ? LocationStatus.ACTIVE : LocationStatus.INACTIVE); site.setDefault(selected);
        return site;
    }
    private ReceivingService service() {
        return new ReceivingService(actor, employees, purchaseOrders, purchaseOrderItems, requests,
                requestItems, operations, operationItems, serials, assignments, warehouseService,
                null, null, organizationProducts, null, sites, zones);
    }
}
