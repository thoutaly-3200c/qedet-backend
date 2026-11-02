package q.qedet.warehouse.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.ConflictException;
import q.qedet.common.exception.ForbiddenException;
import q.qedet.common.exception.NotFoundException;
import q.qedet.common.model.LocationStatus;
import q.qedet.common.model.LocationType;
import q.qedet.common.model.Permission;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.entity.OrganizationSite;
import q.qedet.organization.entity.OrganizationZone;
import q.qedet.organization.repository.OrganizationRepository;
import q.qedet.organization.repository.OrganizationSiteRepository;
import q.qedet.organization.repository.OrganizationZoneRepository;
import q.qedet.product.repository.OrganizationProductRepository;
import q.qedet.sales.entity.SalesStockRequest;
import q.qedet.sales.entity.SalesStockRequestItem;
import q.qedet.sales.repository.SalesStockRequestRepository;
import q.qedet.sales.repository.SalesReceivingRequestRepository;
import q.qedet.sales.entity.SalesReceivingRequest;
import q.qedet.warehouse.dto.FulfillmentDtos;
import q.qedet.warehouse.entity.InventoryBalance;
import q.qedet.warehouse.entity.SalesStockFulfillment;
import q.qedet.warehouse.entity.SalesStockFulfillmentLine;
import q.qedet.warehouse.repository.InventoryBalanceRepository;
import q.qedet.warehouse.repository.SalesStockFulfillmentRepository;
import q.qedet.warehouse.repository.StockMovementRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SalesFulfillmentServiceTest {
    @Mock ActorContext actor;
    @Mock OrganizationRepository organizations;
    @Mock OrganizationSiteRepository sites;
    @Mock OrganizationZoneRepository zones;
    @Mock OrganizationProductRepository organizationProducts;
    @Mock SalesStockRequestRepository requests;
    @Mock SalesStockFulfillmentRepository fulfillments;
    @Mock InventoryBalanceRepository balances;
    @Mock StockMovementRepository movements;
    @Mock SalesReceivingRequestRepository receivingRequests;

    private UUID organizationId;
    private Organization organization;
    private OrganizationSite warehouse;
    private OrganizationSite hall;
    private OrganizationZone sourceA;
    private OrganizationZone sourceB;
    private OrganizationZone destination;
    private SalesStockRequest request;
    private SalesStockRequestItem requestItem;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        organization = new Organization(); organization.setId(organizationId);
        warehouse = site(LocationType.WAREHOUSE); hall = site(LocationType.SALES_HALL);
        sourceA = zone(warehouse); sourceB = zone(warehouse); destination = zone(hall);
        request = new SalesStockRequest(); request.setId(UUID.randomUUID()); request.setOrganization(organization);
        request.setStatus("ACCEPTED");
        requestItem = new SalesStockRequestItem(); requestItem.setProductId(UUID.randomUUID()); requestItem.setRequestedQuantity(new BigDecimal("10.00")); requestItem.setApprovedQuantity(new BigDecimal("10.00")); requestItem.setRequest(request); request.getItems().add(requestItem);
        when(actor.organizationId()).thenReturn(organizationId); when(actor.employeeId()).thenReturn(UUID.randomUUID());
        when(organizations.findById(organizationId)).thenReturn(Optional.of(organization));
        when(sites.findAllByOrganizationIdOrderByNameAsc(organizationId)).thenReturn(List.of(warehouse, hall));
        when(sites.findByIdAndOrganizationId(warehouse.getId(), organizationId)).thenReturn(Optional.of(warehouse));
        when(sites.findByIdAndOrganizationId(hall.getId(), organizationId)).thenReturn(Optional.of(hall));
        when(zones.findByIdAndSiteOrganizationIdAndActiveTrue(destination.getId(), organizationId)).thenReturn(Optional.of(destination));
        when(zones.findByIdAndSiteOrganizationIdAndActiveTrue(sourceA.getId(), organizationId)).thenReturn(Optional.of(sourceA));
        when(zones.findByIdAndSiteOrganizationIdAndActiveTrue(sourceB.getId(), organizationId)).thenReturn(Optional.of(sourceB));
        when(requests.findForUpdateByIdAndOrganizationId(request.getId(), organizationId)).thenReturn(Optional.of(request));
        when(organizationProducts.existsByOrganizationIdAndProductId(organizationId, requestItem.getProductId())).thenReturn(true);
        when(fulfillments.save(any())).thenAnswer(invocation -> { SalesStockFulfillment value = invocation.getArgument(0); if (value.getId() == null) value.setId(UUID.randomUUID()); return value; });
    }

    @Test
    void confirmsPartialFulfillmentAcrossMultipleWarehouseZones() {
        InventoryBalance a = balance(sourceA, "4.00", "0.00");
        InventoryBalance b = balance(sourceB, "3.00", "0.00");
        InventoryBalance destinationBalance = balance(destination, "0.00", "0.00");
        stubBalance(sourceA, a); stubBalance(sourceB, b); stubBalance(destination, destinationBalance);

        var result = service().confirm(request.getId(), confirm("4.00", "3.00"));

        assertThat(result.get("status")).isEqualTo("CONFIRMED");
        assertThat(a.getOnHand()).isEqualByComparingTo("0.00");
        assertThat(b.getOnHand()).isEqualByComparingTo("0.00");
        assertThat(destinationBalance.getOnHand()).isEqualByComparingTo("7.00");
        assertThat(destinationBalance.getReserved()).isEqualByComparingTo("7.00");
        assertThat(requestItem.getFulfilledQuantity()).isEqualByComparingTo("7.00");
        assertThat(request.getStatus()).isEqualTo("PARTIALLY_FULFILLED");
        ArgumentCaptor<SalesReceivingRequest> receiving = ArgumentCaptor.forClass(SalesReceivingRequest.class);
        verify(receivingRequests).save(receiving.capture());
        assertThat(receiving.getValue().getFulfillment().getId()).isEqualTo(result.get("id"));
        assertThat(receiving.getValue().getItems()).hasSize(1);
        assertThat(receiving.getValue().getItems().get(0).getDispatchedQuantity()).isEqualByComparingTo("7.00");
        verify(movements, times(2)).save(any());
    }

    @Test
    void doesNotCreateSecondReceivingRequestForExistingFulfillment() {
        stubBalance(sourceA, balance(sourceA, "4.00", "0.00"));
        stubBalance(sourceB, balance(sourceB, "3.00", "0.00"));
        stubBalance(destination, balance(destination, "0.00", "0.00"));
        when(receivingRequests.existsByFulfillmentId(any())).thenReturn(true);

        service().confirm(request.getId(), confirm("4.00", "3.00"));

        verify(receivingRequests, never()).save(any());
    }

    @Test
    void refusesFulfillmentPastRequestedQuantity() {
        requestItem.setRequestedQuantity(new BigDecimal("6.00"));
        requestItem.setApprovedQuantity(new BigDecimal("6.00"));
        assertThatThrownBy(() -> service().confirm(request.getId(), confirm("4.00", "3.00")))
                .isInstanceOf(ConflictException.class).hasMessageContaining("remaining approved quantity");
        verifyNoInteractions(balances, movements);
    }

    @Test
    void warehouseAcceptanceChangesOnlyRequestStatus() {
        request.setStatus("SUBMITTED");
        var result = service().acceptRequest(request.getId());
        assertThat(result.get("status")).isEqualTo("ACCEPTED");
        verify(actor).requirePermission(Permission.INVENTORY_MANAGE);
        verifyNoInteractions(balances, movements, fulfillments);
        assertThatThrownBy(() -> service().acceptRequest(request.getId()))
                .isInstanceOf(ConflictException.class).hasMessageContaining("Only submitted");
    }

    @Test
    void warehouseAcceptanceRequiresPermissionAndOrganizationOwnership() {
        request.setStatus("SUBMITTED");
        doThrow(new ForbiddenException("Missing permission"))
                .when(actor).requirePermission(Permission.INVENTORY_MANAGE);
        assertThatThrownBy(() -> service().acceptRequest(request.getId())).isInstanceOf(ForbiddenException.class);
        assertThat(request.getStatus()).isEqualTo("SUBMITTED");
        reset(actor);
        when(actor.organizationId()).thenReturn(organizationId);
        when(requests.findForUpdateByIdAndOrganizationId(request.getId(), organizationId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().acceptRequest(request.getId())).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(balances, movements);
    }

    @Test
    void fulfillmentRequiresWarehouseAcceptanceAndApprovedQuantity() {
        request.setStatus("SUBMITTED");
        assertThatThrownBy(() -> service().confirm(request.getId(), confirm("4.00", "3.00")))
                .isInstanceOf(ConflictException.class).hasMessageContaining("Warehouse must accept");
        request.setStatus("ACCEPTED");
        requestItem.setApprovedQuantity(BigDecimal.ZERO);
        stubBalance(sourceA, balance(sourceA, "4.00", "0.00"));
        stubBalance(sourceB, balance(sourceB, "3.00", "0.00"));
        stubBalance(destination, balance(destination, "0.00", "0.00"));
        assertThatThrownBy(() -> service().confirm(request.getId(), confirm("4.00", "3.00")))
                .isInstanceOf(ConflictException.class).hasMessageContaining("remaining approved quantity");
        request.setStatus("SUBMITTED");
        service().acceptRequest(request.getId());
        service().confirm(request.getId(), confirm("4.00", "3.00"));
        assertThat(requestItem.getFulfilledQuantity()).isEqualByComparingTo("7.00");
    }

    @Test
    void fulfillmentRespectsPreviouslyDispatchedQuantityAndAvailableStock() {
        requestItem.setFulfilledQuantity(new BigDecimal("4.00"));
        assertThatThrownBy(() -> service().confirm(request.getId(), confirm("4.00", "3.00")))
                .isInstanceOf(ConflictException.class).hasMessageContaining("remaining approved quantity");
        requestItem.setFulfilledQuantity(BigDecimal.ZERO);
        stubBalance(sourceA, balance(sourceA, "3.00", "0.00"));
        assertThatThrownBy(() -> service().confirm(request.getId(), confirm("4.00", "3.00")))
                .isInstanceOf(ConflictException.class).hasMessageContaining("Insufficient available stock");
    }

    @Test
    void rejectsMissingWarehouse() {
        when(sites.findAllByOrganizationIdOrderByNameAsc(organizationId)).thenReturn(List.of(hall));
        assertThatThrownBy(() -> service().confirm(request.getId(), confirm("4.00", "3.00")))
                .isInstanceOf(ConflictException.class).hasMessageContaining("no warehouse");
    }

    @Test
    void rejectsMultipleWarehouses() {
        when(sites.findAllByOrganizationIdOrderByNameAsc(organizationId))
                .thenReturn(List.of(warehouse, site(LocationType.WAREHOUSE), hall));
        assertThatThrownBy(() -> service().confirm(request.getId(), confirm("4.00", "3.00")))
                .isInstanceOf(ConflictException.class).hasMessageContaining("exactly one warehouse");
    }

    @Test
    void rejectsInactiveWarehouse() {
        warehouse.setStatus(LocationStatus.INACTIVE);
        assertThatThrownBy(() -> service().confirm(request.getId(), confirm("4.00", "3.00")))
                .isInstanceOf(ConflictException.class).hasMessageContaining("must be active");
    }

    @Test
    void rejectsInvalidSalesHall() {
        hall.setStatus(LocationStatus.INACTIVE);
        assertThatThrownBy(() -> service().confirm(request.getId(), confirm("4.00", "3.00")))
                .isInstanceOf(ConflictException.class).hasMessageContaining("Request site must be active");
    }

    @Test
    void rejectsDestinationZoneOutsideSelectedHall() {
        destination.setSite(site(LocationType.SALES_HALL));
        assertThatThrownBy(() -> service().confirm(request.getId(), confirm("4.00", "3.00")))
                .isInstanceOf(ConflictException.class).hasMessageContaining("Destination zone must belong");
    }

    @Test
    void rejectsSourceZoneOutsideResolvedWarehouse() {
        sourceA.setSite(site(LocationType.WAREHOUSE));
        assertThatThrownBy(() -> service().confirm(request.getId(), confirm("4.00", "3.00")))
                .isInstanceOf(ConflictException.class).hasMessageContaining("unique active zone");
    }

    @Test
    void salesAcceptanceClearsPendingQuantityAndKeepsPartialRequestOpen() {
        SalesStockFulfillment fulfillment = rejectedCandidate();
        requestItem.setFulfilledQuantity(new BigDecimal("7.00"));
        requestItem.setPendingAcceptanceQuantity(new BigDecimal("7.00"));
        stubBalance(destination, balance(destination, "7.00", "7.00"));
        when(fulfillments.findForUpdateByIdAndOrganizationId(fulfillment.getId(), organizationId))
                .thenReturn(Optional.of(fulfillment));

        service().accept(fulfillment.getId(), new FulfillmentDtos.Decision("received"));

        assertThat(requestItem.getAcceptedQuantity()).isEqualByComparingTo("7.00");
        assertThat(requestItem.getPendingAcceptanceQuantity()).isEqualByComparingTo("0.00");
        assertThat(request.getStatus()).isEqualTo("PARTIALLY_FULFILLED");
        verifyNoInteractions(movements);
    }

    @Test
    void rejectsThenRestoresStockOnlyWhenPhysicalReturnIsReceived() {
        InventoryBalance a = balance(sourceA, "3.00", "0.00");
        InventoryBalance destinationBalance = balance(destination, "7.00", "7.00");
        stubBalance(sourceA, a); stubBalance(destination, destinationBalance);
        SalesStockFulfillment fulfillment = rejectedCandidate();
        requestItem.setFulfilledQuantity(new BigDecimal("7.00")); requestItem.setPendingAcceptanceQuantity(new BigDecimal("7.00"));
        when(fulfillments.findForUpdateByIdAndOrganizationId(fulfillment.getId(), organizationId)).thenReturn(Optional.of(fulfillment));

        service().reject(fulfillment.getId(), new FulfillmentDtos.Decision("rejected"));
        service().reject(fulfillment.getId(), new FulfillmentDtos.Decision("repeat"));
        assertThat(a.getOnHand()).isEqualByComparingTo("3.00");
        assertThat(fulfillment.getStatus()).isEqualTo("REJECTED");

        service().receivePhysicalReturn(fulfillment.getId(), new FulfillmentDtos.ReturnReceipt(sourceA.getId(), List.of(new FulfillmentDtos.ReturnLine(requestItem.getProductId(), new BigDecimal("7.00")))));
        service().receivePhysicalReturn(fulfillment.getId(), new FulfillmentDtos.ReturnReceipt(sourceA.getId(), List.of(new FulfillmentDtos.ReturnLine(requestItem.getProductId(), new BigDecimal("7.00")))));
        assertThat(a.getOnHand()).isEqualByComparingTo("10.00");
        assertThat(destinationBalance.getOnHand()).isEqualByComparingTo("0.00");
        assertThat(fulfillment.getStatus()).isEqualTo("RETURNED");
        assertThat(requestItem.getFulfilledQuantity()).isEqualByComparingTo("0.00");
        assertThat(requestItem.getPendingAcceptanceQuantity()).isEqualByComparingTo("0.00");
    }


    private SalesStockFulfillment rejectedCandidate() { SalesStockFulfillment fulfillment = new SalesStockFulfillment(); fulfillment.setId(UUID.randomUUID()); fulfillment.setOrganization(organization); fulfillment.setSalesStockRequestId(request.getId()); fulfillment.setWarehouseId(warehouse.getId()); fulfillment.setSalesHallId(hall.getId()); fulfillment.setDestinationZone(destination); fulfillment.setStatus("CONFIRMED"); SalesStockFulfillmentLine line = new SalesStockFulfillmentLine(); line.setFulfillment(fulfillment); line.setProductId(requestItem.getProductId()); line.setQuantity(new BigDecimal("7.00")); fulfillment.getLines().add(line); return fulfillment; }
    private SalesFulfillmentService service() { return new SalesFulfillmentService(actor, organizations, sites, zones, organizationProducts, requests, fulfillments, balances, movements, receivingRequests); }
    private FulfillmentDtos.Confirm confirm(String first, String second) { return new FulfillmentDtos.Confirm(hall.getId(), destination.getId(), List.of(new FulfillmentDtos.Line(requestItem.getProductId(), List.of(new FulfillmentDtos.Allocation(sourceA.getId(), new BigDecimal(first)), new FulfillmentDtos.Allocation(sourceB.getId(), new BigDecimal(second)))))); }
    private void stubBalance(OrganizationZone zone, InventoryBalance value) { when(balances.findForUpdate(organizationId, requestItem.getProductId(), zone.getId())).thenReturn(Optional.of(value)); }
    private InventoryBalance balance(OrganizationZone zone, String onHand, String reserved) { InventoryBalance value = new InventoryBalance(); value.setOrganization(organization); value.setProductId(requestItem.getProductId()); value.setZone(zone); value.setOnHand(new BigDecimal(onHand)); value.setReserved(new BigDecimal(reserved)); return value; }
    private OrganizationSite site(LocationType type) { OrganizationSite value = new OrganizationSite(); value.setId(UUID.randomUUID()); value.setOrganization(organization); value.setType(type); value.setStatus(LocationStatus.ACTIVE); return value; }
    private OrganizationZone zone(OrganizationSite site) { OrganizationZone value = new OrganizationZone(); value.setId(UUID.randomUUID()); value.setSite(site); value.setActive(true); return value; }
}

