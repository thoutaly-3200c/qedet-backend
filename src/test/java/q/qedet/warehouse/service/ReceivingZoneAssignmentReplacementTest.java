package q.qedet.warehouse.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.BadRequestException;
import q.qedet.common.model.LocationStatus;
import q.qedet.common.model.LocationType;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.entity.OrganizationSite;
import q.qedet.organization.entity.OrganizationZone;
import q.qedet.organization.repository.OrganizationSiteRepository;
import q.qedet.organization.repository.OrganizationZoneRepository;
import q.qedet.procurement.repository.PurchaseOrderItemRepository;
import q.qedet.procurement.repository.PurchaseOrderRepository;
import q.qedet.product.repository.OrganizationProductRepository;
import q.qedet.product.repository.ProductRepository;
import q.qedet.product.service.OrganizationProductService;
import q.qedet.qr.service.QrCodeService;
import q.qedet.warehouse.dto.ReceivingDtos;
import q.qedet.warehouse.entity.ReceivingOperation;
import q.qedet.warehouse.entity.ReceivingOperationItem;
import q.qedet.warehouse.entity.ReceivingRequest;
import q.qedet.warehouse.repository.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceivingZoneAssignmentReplacementTest {
    @Mock private ActorContext actor;
    @Mock private EmployeeRepository employees;
    @Mock private PurchaseOrderRepository purchaseOrders;
    @Mock private PurchaseOrderItemRepository purchaseOrderItems;
    @Mock private ReceivingRequestRepository requests;
    @Mock private ReceivingRequestItemRepository requestItems;
    @Mock private ReceivingOperationRepository operations;
    @Mock private ReceivingOperationItemRepository operationItems;
    @Mock private ReceivingOperationSerialRepository serials;
    @Mock private ReceivingOperationZoneAssignmentRepository assignments;
    @Mock private WarehouseService warehouseService;
    @Mock private QrCodeService qrCodes;
    @Mock private ProductRepository products;
    @Mock private OrganizationProductRepository organizationProducts;
    @Mock private OrganizationProductService organizationProductService;
    @Mock private OrganizationSiteRepository sites;
    @Mock private OrganizationZoneRepository zones;

    private ReceivingService service;

    @BeforeEach
    void setUp() {
        service = new ReceivingService(actor, employees, purchaseOrders, purchaseOrderItems, requests, requestItems,
                operations, operationItems, serials, assignments, warehouseService, qrCodes, products,
                organizationProducts, organizationProductService, sites, zones);
    }

    @Test
    void firstAssignmentBulkDeletesAndFlushesBeforeSavingTheValidatedReplacement() {
        Scenario scenario = scenario();
        validZone(scenario);

        service.replaceZoneAssignments(scenario.line(), List.of(assignment(scenario.zone().getId(), "5")), new BigDecimal("5"));

        InOrder order = inOrder(assignments);
        order.verify(assignments).deleteAllByReceivingOperationItemIdInBulk(scenario.line().getId());
        order.verify(assignments).flush();
        order.verify(assignments).save(any());
    }

    @Test
    void repeatedAssignmentUpdateReplacesRowsWithoutCreatingDuplicatePairs() {
        Scenario scenario = scenario();
        validZone(scenario);

        service.replaceZoneAssignments(scenario.line(), List.of(assignment(scenario.zone().getId(), "5")), new BigDecimal("5"));
        service.replaceZoneAssignments(scenario.line(), List.of(assignment(scenario.zone().getId(), "5")), new BigDecimal("5"));

        verify(assignments, times(2)).deleteAllByReceivingOperationItemIdInBulk(scenario.line().getId());
        verify(assignments, times(2)).flush();
        verify(assignments, times(2)).save(any());
    }

    @Test
    void duplicateSubmittedZoneIsRejectedBeforeExistingAssignmentsAreDeleted() {
        Scenario scenario = scenario();

        assertThatThrownBy(() -> service.replaceZoneAssignments(scenario.line(), List.of(
                assignment(scenario.zone().getId(), "2"), assignment(scenario.zone().getId(), "3")), new BigDecimal("5")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("assigned once");

        verifyNoInteractions(assignments, zones, sites);
    }

    @Test
    void zeroAcceptedQuantityClearsAssignmentsAndCreatesNone() {
        Scenario scenario = scenario();

        service.replaceZoneAssignments(scenario.line(), List.of(), BigDecimal.ZERO);

        verify(assignments).deleteAllByReceivingOperationItemIdInBulk(scenario.line().getId());
        verify(assignments).flush();
        verify(assignments, never()).save(any());
    }

    private void validZone(Scenario scenario) {
        when(actor.organizationId()).thenReturn(scenario.organization().getId());
        when(sites.findByIdAndOrganizationId(scenario.site().getId(), scenario.organization().getId()))
                .thenReturn(Optional.of(scenario.site()));
        when(zones.findByIdAndSiteId(scenario.zone().getId(), scenario.site().getId()))
                .thenReturn(Optional.of(scenario.zone()));
    }

    private Scenario scenario() {
        Organization organization = new Organization();
        organization.setId(UUID.randomUUID());
        OrganizationSite site = new OrganizationSite(organization, "WH", "Receiving", LocationType.WAREHOUSE);
        site.setId(UUID.randomUUID());
        site.setStatus(LocationStatus.ACTIVE);
        OrganizationZone zone = new OrganizationZone(site, "STAGE", "Staging");
        zone.setId(UUID.randomUUID());
        ReceivingRequest request = new ReceivingRequest();
        request.setOrganization(organization);
        request.setDestinationLocation(site);
        ReceivingOperation operation = new ReceivingOperation();
        operation.setReceivingRequest(request);
        ReceivingOperationItem line = new ReceivingOperationItem();
        line.setId(UUID.randomUUID());
        line.setReceivingOperation(operation);
        return new Scenario(organization, site, zone, line);
    }

    private ReceivingDtos.ZoneAssignmentRequest assignment(UUID zoneId, String quantity) {
        return new ReceivingDtos.ZoneAssignmentRequest(zoneId, new BigDecimal(quantity));
    }

    private record Scenario(Organization organization, OrganizationSite site, OrganizationZone zone,
                            ReceivingOperationItem line) { }
}
