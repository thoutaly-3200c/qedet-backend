package q.qedet.sales.service;

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
import q.qedet.organization.repository.OrganizationSiteRepository;
import q.qedet.organization.repository.OrganizationZoneRepository;
import q.qedet.product.repository.OrganizationProductRepository;
import q.qedet.sales.dto.SalesDtos;
import q.qedet.sales.entity.SalesReceivingItem;
import q.qedet.sales.entity.SalesReceivingRequest;
import q.qedet.sales.entity.SalesStockRequest;
import q.qedet.sales.entity.SalesStockRequestItem;
import q.qedet.sales.repository.SalesReceivingRequestRepository;
import q.qedet.sales.repository.SalesStockRequestRepository;
import q.qedet.warehouse.entity.InventoryBalance;
import q.qedet.warehouse.entity.SalesStockFulfillment;
import q.qedet.warehouse.entity.StockMovement;
import q.qedet.warehouse.repository.InventoryBalanceRepository;
import q.qedet.warehouse.repository.StockMovementRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SalesReceivingServiceTest {
    @Mock ActorContext actor;
    @Mock SalesReceivingRequestRepository receivingRequests;
    @Mock SalesStockRequestRepository stockRequests;
    @Mock InventoryBalanceRepository balances;
    @Mock OrganizationProductRepository products;
    @Mock OrganizationSiteRepository sites;
    @Mock OrganizationZoneRepository zones;
    @Mock StockMovementRepository movements;

    private UUID organizationId, productId;
    private Organization organization;
    private OrganizationSite hall, warehouse;
    private OrganizationZone dispatchedZone, otherHallZone, warehouseZone, otherWarehouseZone;
    private SalesStockRequest stockRequest;
    private SalesStockRequestItem requestItem;
    private SalesReceivingRequest receiving;
    private InventoryBalance dispatched, otherHall, returned;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID(); productId = UUID.randomUUID();
        organization = new Organization(); organization.setId(organizationId);
        hall = site(LocationType.SALES_HALL); warehouse = site(LocationType.WAREHOUSE);
        dispatchedZone = zone(hall); otherHallZone = zone(hall);
        warehouseZone = zone(warehouse); otherWarehouseZone = zone(warehouse);
        stockRequest = new SalesStockRequest(); stockRequest.setId(UUID.randomUUID());
        stockRequest.setOrganization(organization); 
        
        requestItem = new SalesStockRequestItem(); requestItem.setProductId(productId);
        requestItem.setRequestedQuantity(amount("10")); requestItem.setFulfilledQuantity(amount("10"));
        requestItem.setPendingAcceptanceQuantity(amount("10")); stockRequest.getItems().add(requestItem);
        SalesStockFulfillment fulfillment = new SalesStockFulfillment(); fulfillment.setId(UUID.randomUUID());
        fulfillment.setOrganization(organization); fulfillment.setSalesStockRequestId(stockRequest.getId());
        receiving = new SalesReceivingRequest(); receiving.setId(UUID.randomUUID());
        receiving.setOrganization(organization); receiving.setStockRequest(stockRequest);
        fulfillment.setWarehouseId(warehouse.getId()); fulfillment.setSalesHallId(hall.getId()); receiving.setFulfillment(fulfillment); receiving.setSalesHallId(hall.getId());
        receiving.setDestinationZone(dispatchedZone);
        SalesReceivingItem item = new SalesReceivingItem(); item.setProductId(productId);
        item.setDispatchedQuantity(amount("10")); item.setReceivingRequest(receiving); receiving.getItems().add(item);
        dispatched = balance(dispatchedZone, "10", "10");
        otherHall = balance(otherHallZone, "2", "0");
        returned = balance(warehouseZone, "3", "0");
        when(actor.organizationId()).thenReturn(organizationId);
        when(receivingRequests.findForUpdate(receiving.getId(), organizationId)).thenReturn(Optional.of(receiving));
        when(stockRequests.findForUpdateByIdAndOrganizationId(stockRequest.getId(), organizationId))
                .thenReturn(Optional.of(stockRequest));
        when(products.existsByOrganizationIdAndProductId(organizationId, productId)).thenReturn(true);
        when(sites.findByIdAndOrganizationId(hall.getId(), organizationId)).thenReturn(Optional.of(hall));
        when(sites.findByIdAndOrganizationId(warehouse.getId(), organizationId)).thenReturn(Optional.of(warehouse));
        for (OrganizationZone zone : List.of(dispatchedZone, otherHallZone, warehouseZone, otherWarehouseZone)) {
            when(zones.findByIdAndSiteOrganizationIdAndActiveTrue(zone.getId(), organizationId))
                    .thenReturn(Optional.of(zone));
        }
        when(balances.findForUpdate(organizationId, productId, dispatchedZone.getId())).thenReturn(Optional.of(dispatched));
        when(balances.findForUpdate(organizationId, productId, otherHallZone.getId())).thenReturn(Optional.of(otherHall));
        when(balances.findForUpdate(organizationId, productId, warehouseZone.getId())).thenReturn(Optional.of(returned));
    }

    @Test
    void acceptsAllWithoutAddingOnHandAgain() {
        var result = service().acceptReceivingRequest(receiving.getId(), confirmation("10", "0", dispatchedZone, warehouseZone));
        assertThat(result.get("status")).isEqualTo("ACCEPTED");
        assertThat(dispatched.getOnHand()).isEqualByComparingTo("10");
        assertThat(dispatched.getReserved()).isEqualByComparingTo("0");
        assertThat(returned.getOnHand()).isEqualByComparingTo("3");
        assertThat(requestItem.getFulfilledQuantity()).isEqualByComparingTo("10");
        assertThat(requestItem.getAcceptedQuantity()).isEqualByComparingTo("10");
        assertThat(stockRequest.getStatus()).isEqualTo("FULFILLED");
        assertThat(receiving.getFulfillment().getStatus()).isEqualTo("RECEIVED");
        assertMovements("SALES_RECEIVING_ACCEPTED");
    }

    @Test
    void rejectsAllAndReturnsStockToWarehouse() {
        var result = service().acceptReceivingRequest(receiving.getId(), confirmation("0", "10", dispatchedZone, warehouseZone));
        assertThat(result.get("status")).isEqualTo("REJECTED");
        assertThat(dispatched.getOnHand()).isEqualByComparingTo("0");
        assertThat(dispatched.getReserved()).isEqualByComparingTo("0");
        assertThat(returned.getOnHand()).isEqualByComparingTo("13");
        assertThat(requestItem.getFulfilledQuantity()).isEqualByComparingTo("0");
        assertThat(requestItem.getPendingAcceptanceQuantity()).isEqualByComparingTo("0");
        assertThat(requestItem.getAcceptedQuantity()).isEqualByComparingTo("0");
        assertThat(receiving.getFulfillment().getStatus()).isEqualTo("RECEIVED");
        assertMovements("SALES_RECEIVING_REJECTED_RETURN");
    }

    @Test
    void splitsAcceptedAndRejectedWithoutDuplicatingStock() {
        service().acceptReceivingRequest(receiving.getId(), confirmation("7", "3", dispatchedZone, warehouseZone));
        assertThat(receiving.getStatus()).isEqualTo("PARTIALLY_ACCEPTED");
        assertThat(dispatched.getOnHand()).isEqualByComparingTo("7");
        assertThat(dispatched.getReserved()).isEqualByComparingTo("0");
        assertThat(returned.getOnHand()).isEqualByComparingTo("6");
        assertThat(requestItem.getFulfilledQuantity()).isEqualByComparingTo("7");
        assertThat(requestItem.getAcceptedQuantity()).isEqualByComparingTo("7");
        assertThat(receiving.getItems().get(0).getDiscrepancyQuantity()).isEqualByComparingTo("3");
        assertMovements("SALES_RECEIVING_ACCEPTED", "SALES_RECEIVING_REJECTED_RETURN");
    }

    @Test
    void movesAcceptedStockToAnotherHallZone() {
        service().acceptReceivingRequest(receiving.getId(), confirmation("7", "3", otherHallZone, warehouseZone));
        assertThat(dispatched.getOnHand()).isEqualByComparingTo("0");
        assertThat(dispatched.getReserved()).isEqualByComparingTo("0");
        assertThat(otherHall.getOnHand()).isEqualByComparingTo("9");
        assertThat(returned.getOnHand()).isEqualByComparingTo("6");
        assertThat(receiving.getItems().get(0).getAcceptedZoneId()).isEqualTo(otherHallZone.getId());
    }

    @Test
    void rejectsCrossSiteAndInactiveDestinations() {
        assertThatThrownBy(() -> service().acceptReceivingRequest(receiving.getId(),
                confirmation("7", "3", warehouseZone, warehouseZone))).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service().acceptReceivingRequest(receiving.getId(),
                confirmation("7", "3", dispatchedZone, otherHallZone))).isInstanceOf(ConflictException.class);
        otherHallZone.setActive(false);
        assertThatThrownBy(() -> service().acceptReceivingRequest(receiving.getId(),
                confirmation("7", "3", otherHallZone, warehouseZone))).isInstanceOf(ConflictException.class);
        verifyNoInteractions(movements);
        assertThat(dispatched.getReserved()).isEqualByComparingTo("10");
    }

    @Test
    void rejectsInvalidQuantitiesAndUnknownOrDuplicateProducts() {
        assertThatThrownBy(() -> service().acceptReceivingRequest(receiving.getId(),
                confirmation("8", "3", dispatchedZone, warehouseZone))).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service().acceptReceivingRequest(receiving.getId(),
                confirmation("-1", "11", dispatchedZone, warehouseZone))).isInstanceOf(ConflictException.class);
        var line = line(productId, "7", "3", dispatchedZone, warehouseZone);
        assertThatThrownBy(() -> service().acceptReceivingRequest(receiving.getId(),
                new SalesDtos.ReceivingConfirmation(List.of(line, line), null))).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service().acceptReceivingRequest(receiving.getId(),
                new SalesDtos.ReceivingConfirmation(List.of(line(UUID.randomUUID(), "7", "3", dispatchedZone, warehouseZone)), null)))
                .isInstanceOf(ConflictException.class);
        verifyNoInteractions(movements);
    }

    @Test
    void preventsRepeatedDecisionUnderReceivingRowLock() {
        service().acceptReceivingRequest(receiving.getId(), confirmation("10", "0", dispatchedZone, warehouseZone));
        assertThatThrownBy(() -> service().acceptReceivingRequest(receiving.getId(),
                confirmation("10", "0", dispatchedZone, warehouseZone))).isInstanceOf(ConflictException.class);
        verify(receivingRequests, times(2)).findForUpdate(receiving.getId(), organizationId);
        verify(movements, times(1)).save(any());
    }

    @Test
    void retainsPendingQuantityFromAnotherFulfillment() {
        requestItem.setRequestedQuantity(amount("20"));
        requestItem.setFulfilledQuantity(amount("20"));
        requestItem.setPendingAcceptanceQuantity(amount("20"));
        service().acceptReceivingRequest(receiving.getId(), confirmation("7", "3", dispatchedZone, warehouseZone));
        assertThat(requestItem.getFulfilledQuantity()).isEqualByComparingTo("17");
        assertThat(requestItem.getPendingAcceptanceQuantity()).isEqualByComparingTo("10");
        assertThat(requestItem.getAcceptedQuantity()).isEqualByComparingTo("7");
        assertThat(stockRequest.getStatus()).isEqualTo("PENDING_ACCEPTANCE");
    }

    @Test
    void confirmsTwoFulfillmentsOfOneStockRequestIndependently() {
        requestItem.setRequestedQuantity(amount("20"));
        requestItem.setFulfilledQuantity(amount("20"));
        requestItem.setPendingAcceptanceQuantity(amount("20"));
        SalesStockFulfillment secondFulfillment = new SalesStockFulfillment();
        secondFulfillment.setId(UUID.randomUUID());
        secondFulfillment.setOrganization(organization);
        secondFulfillment.setSalesStockRequestId(stockRequest.getId());
        secondFulfillment.setWarehouseId(warehouse.getId());
        secondFulfillment.setSalesHallId(hall.getId());
        SalesReceivingRequest second = new SalesReceivingRequest();
        second.setId(UUID.randomUUID());
        second.setOrganization(organization);
        second.setStockRequest(stockRequest);
        second.setFulfillment(secondFulfillment);
        second.setSalesHallId(hall.getId());
        second.setDestinationZone(otherHallZone);
        SalesReceivingItem secondItem = new SalesReceivingItem();
        secondItem.setReceivingRequest(second);
        secondItem.setProductId(productId);
        secondItem.setDispatchedQuantity(amount("10"));
        second.getItems().add(secondItem);
        otherHall.setOnHand(amount("10"));
        otherHall.setReserved(amount("10"));
        when(receivingRequests.findForUpdate(second.getId(), organizationId)).thenReturn(Optional.of(second));

        service().acceptReceivingRequest(receiving.getId(), confirmation("7", "3", dispatchedZone, warehouseZone));
        service().acceptReceivingRequest(second.getId(), confirmation("10", "0", otherHallZone, warehouseZone));

        assertThat(receiving.getStatus()).isEqualTo("PARTIALLY_ACCEPTED");
        assertThat(second.getStatus()).isEqualTo("ACCEPTED");
        assertThat(requestItem.getFulfilledQuantity()).isEqualByComparingTo("17");
        assertThat(requestItem.getPendingAcceptanceQuantity()).isEqualByComparingTo("0");
        assertThat(requestItem.getAcceptedQuantity()).isEqualByComparingTo("17");
        assertThat(dispatched.getOnHand().add(otherHall.getOnHand()).add(returned.getOnHand()))
                .isEqualByComparingTo("23");
    }

    @Test
    void checksOrganizationAndPermission() {
        assertThatThrownBy(() -> service().get(receiving.getId())).isInstanceOf(NotFoundException.class);
        verify(actor).requirePermission(Permission.SALES_VIEW);
        doThrow(new ForbiddenException("Missing Sales permission"))
                .when(actor).requirePermission(Permission.SALES_MANAGE);
        assertThatThrownBy(() -> service().acceptReceivingRequest(receiving.getId(),
                confirmation("10", "0", dispatchedZone, warehouseZone))).isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(movements);
    }

    private SalesReceivingService service() {
        return new SalesReceivingService(actor, receivingRequests, stockRequests, balances, products, sites, zones, movements);
    }

    private SalesDtos.ReceivingConfirmation confirmation(String accepted, String rejected,
                                                          OrganizationZone hallZone, OrganizationZone returnZone) {
        return new SalesDtos.ReceivingConfirmation(List.of(line(productId, accepted, rejected, hallZone, returnZone)), null);
    }

    private SalesDtos.ReceivingLine line(UUID product, String accepted, String rejected,
                                         OrganizationZone hallZone, OrganizationZone returnZone) {
        return new SalesDtos.ReceivingLine(product, amount(accepted), amount(rejected), hallZone.getId(), returnZone.getId());
    }

    private void assertMovements(String... types) {
        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(movements, times(types.length)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(StockMovement::getMovementType).containsExactlyInAnyOrder(types);
    }

    private OrganizationSite site(LocationType type) {
        OrganizationSite site = new OrganizationSite(); site.setId(UUID.randomUUID());
        site.setOrganization(organization); site.setType(type); site.setStatus(LocationStatus.ACTIVE); return site;
    }

    private OrganizationZone zone(OrganizationSite site) {
        OrganizationZone zone = new OrganizationZone(); zone.setId(UUID.randomUUID());
        zone.setSite(site); zone.setActive(true); return zone;
    }

    private InventoryBalance balance(OrganizationZone zone, String onHand, String reserved) {
        InventoryBalance balance = new InventoryBalance(); balance.setOrganization(organization);
        balance.setProductId(productId); balance.setZone(zone);
        balance.setOnHand(amount(onHand)); balance.setReserved(amount(reserved)); return balance;
    }

    private BigDecimal amount(String value) { return new BigDecimal(value); }
}

