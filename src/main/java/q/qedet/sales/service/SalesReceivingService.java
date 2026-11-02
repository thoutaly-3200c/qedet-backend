package q.qedet.sales.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.ConflictException;
import q.qedet.common.exception.NotFoundException;
import q.qedet.common.model.Permission;
import q.qedet.common.model.LocationStatus;
import q.qedet.common.model.LocationType;
import q.qedet.organization.entity.OrganizationZone;
import q.qedet.organization.repository.OrganizationSiteRepository;
import q.qedet.organization.repository.OrganizationZoneRepository;
import q.qedet.product.repository.OrganizationProductRepository;
import q.qedet.sales.dto.SalesDtos;
import q.qedet.sales.entity.SalesReceivingItem;
import q.qedet.sales.entity.SalesReceivingRequest;
import q.qedet.sales.entity.SalesStockRequestItem;
import q.qedet.sales.repository.SalesReceivingRequestRepository;
import q.qedet.sales.repository.SalesStockRequestRepository;
import q.qedet.warehouse.entity.InventoryBalance;
import q.qedet.warehouse.entity.StockMovement;
import q.qedet.warehouse.repository.InventoryBalanceRepository;
import q.qedet.warehouse.repository.StockMovementRepository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SalesReceivingService {
    private final ActorContext actor;
    private final SalesReceivingRequestRepository receivingRequests;
    private final SalesStockRequestRepository stockRequests;
    private final InventoryBalanceRepository balances;
    private final OrganizationProductRepository products;
    private final OrganizationSiteRepository sites;
    private final OrganizationZoneRepository zones;
    private final StockMovementRepository movements;

    public SalesReceivingService(ActorContext actor, SalesReceivingRequestRepository receivingRequests,
                                 SalesStockRequestRepository stockRequests, InventoryBalanceRepository balances,
                                 OrganizationProductRepository products, OrganizationSiteRepository sites,
                                 OrganizationZoneRepository zones, StockMovementRepository movements) {
        this.actor = actor;
        this.receivingRequests = receivingRequests;
        this.stockRequests = stockRequests;
        this.balances = balances;
        this.products = products;
        this.sites = sites;
        this.zones = zones;
        this.movements = movements;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> pending() {
        actor.requirePermission(Permission.SALES_VIEW);
        return receivingRequests.findAllByOrganizationIdAndStatusOrderByCreatedAtDesc(actor.organizationId(), "PENDING")
                .stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(UUID id) {
        actor.requirePermission(Permission.SALES_VIEW);
        return view(receivingRequests.findByIdAndOrganizationId(id, actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Sales receiving request not found")));
    }

    @Transactional
    public Map<String, Object> acceptReceivingRequest(UUID id, SalesDtos.ReceivingConfirmation input) {
        actor.requirePermission(Permission.SALES_MANAGE);
        SalesReceivingRequest receiving = receivingRequests.findForUpdate(id, actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Sales receiving request not found"));
        if (!"PENDING".equals(receiving.getStatus()) || !"CONFIRMED".equals(receiving.getFulfillment().getStatus())) {
            throw new ConflictException("SALES_RECEIVING_NOT_PENDING", "Receiving request is no longer pending");
        }
        var request = stockRequests.findForUpdateByIdAndOrganizationId(
                receiving.getStockRequest().getId(), actor.organizationId())
                .orElseThrow(() -> new NotFoundException("Sales stock request not found"));
        validateSites(receiving, request);
        Map<UUID, SalesDtos.ReceivingLine> lines = new HashMap<>();
        for (SalesDtos.ReceivingLine line : input.items()) {
            if (line.productId() == null || lines.putIfAbsent(line.productId(), line) != null) {
                throw new ConflictException("SALES_RECEIVING_DUPLICATE_PRODUCT", "Each product must appear exactly once");
            }
        }
        if (lines.size() != receiving.getItems().size()) {
            throw new ConflictException("SALES_RECEIVING_ITEMS_MISMATCH", "Supply one line for every dispatched product");
        }
        Map<UUID, Destinations> destinations = new HashMap<>();
        for (SalesReceivingItem item : receiving.getItems()) {
            SalesDtos.ReceivingLine line = lines.get(item.getProductId());
            if (line == null || !products.existsByOrganizationIdAndProductId(actor.organizationId(), item.getProductId())) {
                throw new ConflictException("SALES_RECEIVING_INVALID_PRODUCT", "Product is not on this receiving request or organization");
            }
            if (quantity(line.acceptedQuantity()).add(quantity(line.rejectedQuantity()))
                    .compareTo(item.getDispatchedQuantity()) != 0) {
                throw new ConflictException("SALES_RECEIVING_QUANTITY_MISMATCH", "Accepted and rejected quantities must equal dispatched quantity");
            }

            OrganizationZone acceptedZone =
                    quantity(line.acceptedQuantity()).signum() > 0
                            ? activeZone(
                            line.salesHallZoneId(),
                            receiving.getFulfillment().getSalesHallId(),
                            "Sales Hall")
                            : null;

            OrganizationZone rejectedZone =
                    quantity(line.rejectedQuantity()).signum() > 0
                            ? activeZone(
                            line.warehouseZoneId(),
                            receiving.getFulfillment().getWarehouseId(),
                            "warehouse")
                            : null;

            destinations.put(
                    item.getProductId(),
                    new Destinations(acceptedZone, rejectedZone)
            );
        }

        boolean anyAccepted = false;
        boolean anyRejected = false;
        for (SalesReceivingItem item : receiving.getItems()) {
            SalesDtos.ReceivingLine line = lines.get(item.getProductId());
            Destinations destination = destinations.get(item.getProductId());
            BigDecimal accepted = line.acceptedQuantity();
            BigDecimal rejected = line.rejectedQuantity();
            OrganizationZone dispatchedZone = receiving.getDestinationZone();
            InventoryBalance dispatched = balances.findForUpdate(actor.organizationId(), item.getProductId(), dispatchedZone.getId())
                    .orElseThrow(() -> new ConflictException("SALES_RECEIVING_STOCK_MISSING", "Dispatched stock is missing"));
            if (zero(dispatched.getOnHand()).compareTo(item.getDispatchedQuantity()) < 0
                    || zero(dispatched.getReserved()).compareTo(item.getDispatchedQuantity()) < 0) {
                throw new ConflictException("SALES_RECEIVING_STOCK_MISSING", "Dispatched stock is no longer fully on hand and reserved");
            }
            BigDecimal transfer = dispatchedZone.getId().equals(destination.acceptedZone().getId())
                    ? BigDecimal.ZERO : accepted;
            dispatched.setOnHand(zero(dispatched.getOnHand()).subtract(rejected).subtract(transfer));
            dispatched.setReserved(zero(dispatched.getReserved()).subtract(item.getDispatchedQuantity()));
            balances.save(dispatched);
            if (transfer.signum() > 0) {
                InventoryBalance placed = balanceForUpdate(item.getProductId(), destination.acceptedZone(), request);
                placed.setOnHand(zero(placed.getOnHand()).add(transfer));
                balances.save(placed);
            }
            if (accepted.signum() > 0) {
                anyAccepted = true;
                movement(receiving, item.getProductId(), dispatchedZone, destination.acceptedZone(), accepted,
                        "SALES_RECEIVING_ACCEPTED");
            }
            if (rejected.signum() > 0) {
                anyRejected = true;
                InventoryBalance returned = balanceForUpdate(item.getProductId(), destination.rejectedZone(), request);
                returned.setOnHand(zero(returned.getOnHand()).add(rejected));
                balances.save(returned);
                movement(receiving, item.getProductId(), dispatchedZone, destination.rejectedZone(), rejected,
                        "SALES_RECEIVING_REJECTED_RETURN");
            }
            SalesStockRequestItem requestItem = request.getItems().stream()
                    .filter(candidate -> candidate.getProductId().equals(item.getProductId())).findFirst()
                    .orElseThrow(() -> new ConflictException("SALES_RECEIVING_ITEMS_MISMATCH", "Product is not on the stock request"));
            if (zero(requestItem.getPendingAcceptanceQuantity()).compareTo(item.getDispatchedQuantity()) < 0
                    || zero(requestItem.getFulfilledQuantity()).compareTo(rejected) < 0) {
                throw new ConflictException("SALES_RECEIVING_PENDING_MISMATCH", "Stock request quantities are inconsistent");
            }
            requestItem.setPendingAcceptanceQuantity(zero(requestItem.getPendingAcceptanceQuantity()).subtract(item.getDispatchedQuantity()));
            requestItem.setFulfilledQuantity(zero(requestItem.getFulfilledQuantity()).subtract(rejected));
            requestItem.setAcceptedQuantity(zero(requestItem.getAcceptedQuantity()).add(accepted));
            item.setActualReceivedQuantity(accepted);
            item.setDiscrepancyQuantity(rejected);
            item.setAcceptedZoneId(destination.acceptedZone().getId());
            item.setRejectedZoneId(
                    destination.rejectedZone() != null
                            ? destination.rejectedZone().getId()
                            : null
            );
        }
        receiving.setStatus(!anyRejected ? "ACCEPTED" : !anyAccepted ? "REJECTED" : "PARTIALLY_ACCEPTED");
        receiving.setAcceptedBy(actor.employeeId());
        receiving.setNotes(input.notes());
        receiving.getFulfillment().setStatus("RECEIVED");
        receiving.getFulfillment().setDecidedBy(actor.employeeId());
        boolean allAccepted = request.getItems().stream().allMatch(item ->
                zero(item.getAcceptedQuantity()).compareTo(item.getRequestedQuantity()) == 0);
        boolean anyPending = request.getItems().stream().anyMatch(item -> zero(item.getPendingAcceptanceQuantity()).signum() > 0);
        request.setStatus(allAccepted ? "FULFILLED" : anyPending ? "PENDING_ACCEPTANCE" : "PARTIALLY_FULFILLED");
        return view(receiving);
    }

    private void validateSites(SalesReceivingRequest receiving, q.qedet.sales.entity.SalesStockRequest request) {
        if (!receiving.getFulfillment().getSalesHallId().equals(receiving.getSalesHallId())
                || !request.getId().equals(receiving.getFulfillment().getSalesStockRequestId())
                || receiving.getFulfillment().getOrganization() == null
                || !actor.organizationId().equals(receiving.getFulfillment().getOrganization().getId())) {
            throw new ConflictException("SALES_RECEIVING_REQUEST_MISMATCH", "Receiving request does not match the fulfillment or Sales Hall");
        }
        activeSite(receiving.getFulfillment().getSalesHallId(), LocationType.SALES_HALL);
        activeSite(receiving.getFulfillment().getWarehouseId(), LocationType.WAREHOUSE);
        if (!receiving.getDestinationZone().isActive()
                || !receiving.getDestinationZone().getSite().getId().equals(receiving.getFulfillment().getSalesHallId())) {
            throw new ConflictException("SALES_RECEIVING_INVALID_DISPATCH_ZONE", "Fulfillment destination is outside the Sales Hall");
        }
    }

    private void activeSite(UUID id, LocationType type) {
        sites.findByIdAndOrganizationId(id, actor.organizationId())
                .filter(site -> site.getType() == type && site.getStatus() == LocationStatus.ACTIVE)
                .orElseThrow(() -> new ConflictException("SALES_RECEIVING_INVALID_SITE", "Request site is inactive or outside the organization"));
    }

    private OrganizationZone activeZone(UUID id, UUID siteId, String label) {
        return zones.findByIdAndSiteOrganizationIdAndActiveTrue(id, actor.organizationId())
                .filter(zone -> zone.isActive() && zone.getSite().getId().equals(siteId)
                        && zone.getSite().getStatus() == LocationStatus.ACTIVE)
                .orElseThrow(() -> new ConflictException("SALES_RECEIVING_INVALID_ZONE", label + " zone must be active and belong to the request site"));
    }

    private InventoryBalance balanceForUpdate(UUID productId, OrganizationZone zone,
                                              q.qedet.sales.entity.SalesStockRequest request) {
        return balances.findForUpdate(actor.organizationId(), productId, zone.getId()).orElseGet(() -> {
            InventoryBalance balance = new InventoryBalance();
            balance.setOrganization(request.getOrganization());
            balance.setProductId(productId);
            balance.setZone(zone);
            return balance;
        });
    }

    private void movement(SalesReceivingRequest receiving, UUID productId, OrganizationZone from,
                          OrganizationZone to, BigDecimal quantity, String type) {
        StockMovement movement = new StockMovement();
        movement.setOrganization(receiving.getOrganization());
        movement.setProductId(productId);
        movement.setFromZone(from);
        movement.setToZone(to);
        movement.setQuantity(quantity);
        movement.setMovementType(type);
        movement.setReference("SALES_RECEIVING:" + receiving.getId());
        movements.save(movement);
    }

    private BigDecimal quantity(BigDecimal value) {
        if (value == null || value.signum() < 0 || value.scale() > 2
                || value.precision() - value.scale() > 12) {
            throw new ConflictException("SALES_RECEIVING_INVALID_QUANTITY", "Quantity must be nonnegative with at most two decimal places");
        }
        return value;
    }

    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }

    private Map<String, Object> view(SalesReceivingRequest receiving) {
        return Map.of("id", receiving.getId(), "stockRequestId", receiving.getStockRequest().getId(),
                "fulfillmentId", receiving.getFulfillment().getId(), "salesHallId", receiving.getSalesHallId(),
                "warehouseId", receiving.getFulfillment().getWarehouseId(),
                "destinationZoneId", receiving.getDestinationZone().getId(), "status", receiving.getStatus(),
                "items", receiving.getItems().stream().map(item -> Map.<String, Object>of(
                        "productId", item.getProductId(), "dispatchedQuantity", item.getDispatchedQuantity(),
                        "acceptedQuantity", zero(item.getActualReceivedQuantity()),
                        "rejectedQuantity", item.getDiscrepancyQuantity(),
                        "salesHallZoneId", item.getAcceptedZoneId() == null ? "" : item.getAcceptedZoneId().toString(),
                        "warehouseZoneId", item.getRejectedZoneId() == null ? "" : item.getRejectedZoneId().toString())).toList());
    }

    private record Destinations(OrganizationZone acceptedZone, OrganizationZone rejectedZone) {}
}
