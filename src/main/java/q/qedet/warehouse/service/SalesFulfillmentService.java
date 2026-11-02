package q.qedet.warehouse.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.BadRequestException;
import q.qedet.common.exception.ConflictException;
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
import q.qedet.sales.entity.SalesReceivingItem;
import q.qedet.sales.entity.SalesReceivingRequest;
import q.qedet.sales.repository.SalesReceivingRequestRepository;
import q.qedet.sales.entity.SalesStockRequestItem;
import q.qedet.sales.repository.SalesStockRequestRepository;
import q.qedet.warehouse.dto.FulfillmentDtos;
import q.qedet.warehouse.entity.InventoryBalance;
import q.qedet.warehouse.entity.SalesStockFulfillment;
import q.qedet.warehouse.entity.SalesStockFulfillmentAllocation;
import q.qedet.warehouse.entity.SalesStockFulfillmentLine;
import q.qedet.warehouse.entity.StockMovement;
import q.qedet.warehouse.repository.InventoryBalanceRepository;
import q.qedet.warehouse.repository.SalesStockFulfillmentRepository;
import q.qedet.warehouse.repository.StockMovementRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Applies physical warehouse-to-Sales-Hall transfers for Sales stock requests. */
@Service
public class SalesFulfillmentService {
    private static final String SUBMITTED = "SUBMITTED";
    private static final String REQUEST_ACCEPTED = "ACCEPTED";
    private static final String PARTIALLY_FULFILLED = "PARTIALLY_FULFILLED";
    private static final String PENDING_ACCEPTANCE = "PENDING_ACCEPTANCE";
    private static final String FULFILLED = "FULFILLED";
    private static final String CANCELLED = "CANCELLED";
    private static final String CONFIRMED = "CONFIRMED";
    private static final String ACCEPTED = "ACCEPTED";
    private static final String REJECTED = "REJECTED";
    private static final String RETURNED = "RETURNED";

    private final ActorContext actor;
    private final OrganizationRepository organizations;
    private final OrganizationSiteRepository sites;
    private final OrganizationZoneRepository zones;
    private final OrganizationProductRepository organizationProducts;
    private final SalesStockRequestRepository requests;
    private final SalesStockFulfillmentRepository fulfillments;
    private final InventoryBalanceRepository balances;
    private final StockMovementRepository movements;
    private final SalesReceivingRequestRepository receivingRequests;

    public SalesFulfillmentService(ActorContext actor, OrganizationRepository organizations,
                                   OrganizationSiteRepository sites, OrganizationZoneRepository zones,
                                   OrganizationProductRepository organizationProducts,
                                   SalesStockRequestRepository requests, SalesStockFulfillmentRepository fulfillments,
                                   InventoryBalanceRepository balances, StockMovementRepository movements,
                                   SalesReceivingRequestRepository receivingRequests) {
        this.actor = actor;
        this.organizations = organizations;
        this.sites = sites;
        this.zones = zones;
        this.organizationProducts = organizationProducts;
        this.requests = requests;
        this.fulfillments = fulfillments;
        this.balances = balances;
        this.movements = movements;
        this.receivingRequests = receivingRequests;
    }

    @Transactional
    public Map<String, Object> acceptRequest(UUID requestId) {
        actor.requirePermission(Permission.INVENTORY_MANAGE);
        SalesStockRequest request = requestForUpdate(requestId);
        requireRequestOrganization(request);
        if (!SUBMITTED.equals(request.getStatus())) {
            throw new ConflictException("SALES_STOCK_REQUEST_NOT_SUBMITTED", "Only submitted Sales stock requests can be accepted by Warehouse");
        }
        request.setStatus(REQUEST_ACCEPTED);
        request.getItems().forEach(item -> item.setApprovedQuantity(item.getRequestedQuantity()));
        return requestView(request);
    }

    @Transactional
    public Map<String, Object> confirm(UUID requestId, FulfillmentDtos.Confirm input) {
        actor.requirePermission(Permission.INVENTORY_MANAGE);
        SalesStockRequest request = requestForUpdate(requestId);
        requireOpenRequest(request);
        OrganizationSite warehouse = resolveOrganizationWarehouse();
        OrganizationSite salesHall = site(input.salesHallId(), LocationType.SALES_HALL);
        OrganizationZone destination = activeZone(input.destinationZoneId());
        if (!destination.getSite().getId().equals(salesHall.getId())) {
            throw new ConflictException("FULFILLMENT_INVALID_DESTINATION", "Destination zone must belong to the Sales Hall");
        }


        SalesStockFulfillment fulfillment = new SalesStockFulfillment();
        fulfillment.setOrganization(organization());
        fulfillment.setSalesStockRequestId(request.getId());
        fulfillment.setWarehouseId(warehouse.getId());
        fulfillment.setSalesHallId(salesHall.getId());
        fulfillment.setDestinationZone(destination);
        fulfillment.setConfirmedBy(actor.employeeId());
        fulfillment = fulfillments.save(fulfillment);

        Set<UUID> productIds = new HashSet<>();
        for (FulfillmentDtos.Line inputLine : input.lines()) {
            if (!productIds.add(inputLine.productId())) {
                throw new BadRequestException("FULFILLMENT_DUPLICATE_PRODUCT", "A product may appear only once in a fulfillment");
            }
            requireOrganizationProduct(inputLine.productId());
            SalesStockRequestItem requestLine = requestItem(request, inputLine.productId());
            BigDecimal total = inputLine.allocations().stream()
                    .map(FulfillmentDtos.Allocation::quantity).map(this::quantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (total.signum() <= 0 || total.add(zero(requestLine.getFulfilledQuantity()))
                    .compareTo(zero(requestLine.getApprovedQuantity())) > 0) {
                throw new ConflictException("FULFILLMENT_EXCEEDS_APPROVED", "Fulfillment exceeds the remaining approved quantity");
            }

            SalesStockFulfillmentLine line = new SalesStockFulfillmentLine();
            line.setFulfillment(fulfillment);
            line.setProductId(inputLine.productId());
            line.setQuantity(total);
            Set<UUID> sourceZoneIds = new HashSet<>();
            for (FulfillmentDtos.Allocation allocation : inputLine.allocations()) {
                BigDecimal allocated = quantity(allocation.quantity());
                OrganizationZone source = activeZone(allocation.sourceZoneId());
                if (!source.getSite().getId().equals(warehouse.getId()) || !sourceZoneIds.add(source.getId())) {
                    throw new ConflictException("FULFILLMENT_INVALID_SOURCE", "Each source must be a unique active zone of the request warehouse");
                }
                InventoryBalance sourceBalance = balanceForUpdate(inputLine.productId(), source);
                if (available(sourceBalance).compareTo(allocated) < 0) {
                    throw new ConflictException("FULFILLMENT_INSUFFICIENT_STOCK", "Insufficient available stock in source zone");
                }
                sourceBalance.setOnHand(zero(sourceBalance.getOnHand()).subtract(allocated));
                balances.save(sourceBalance);

                SalesStockFulfillmentAllocation persistedAllocation = new SalesStockFulfillmentAllocation();
                persistedAllocation.setLine(line);
                persistedAllocation.setSourceZone(source);
                persistedAllocation.setQuantity(allocated);
                line.getAllocations().add(persistedAllocation);
                movement(inputLine.productId(), source, destination, allocated,
                        "SALES_FULFILLMENT_PENDING", reference(fulfillment));
            }

            InventoryBalance destinationBalance = balanceForUpdate(inputLine.productId(), destination);
            destinationBalance.setOnHand(zero(destinationBalance.getOnHand()).add(total));
            destinationBalance.setReserved(zero(destinationBalance.getReserved()).add(total));
            balances.save(destinationBalance);
            requestLine.setFulfilledQuantity(zero(requestLine.getFulfilledQuantity()).add(total));
            requestLine.setPendingAcceptanceQuantity(zero(requestLine.getPendingAcceptanceQuantity()).add(total));
            fulfillment.getLines().add(line);
        }
        request.setStatus(requestStatusAfterConfirmation(request));
        fulfillment = fulfillments.save(fulfillment);
        if (!receivingRequests.existsByFulfillmentId(fulfillment.getId())) {
            SalesReceivingRequest receiving = new SalesReceivingRequest();
            receiving.setOrganization(request.getOrganization());
            receiving.setStockRequest(request);
            receiving.setFulfillment(fulfillment);
            receiving.setSalesHallId(salesHall.getId());
            receiving.setDestinationZone(destination);
            for (SalesStockFulfillmentLine line : fulfillment.getLines()) {
                SalesReceivingItem item = new SalesReceivingItem();
                item.setReceivingRequest(receiving);
                item.setProductId(line.getProductId());
                item.setDispatchedQuantity(line.getQuantity());
                receiving.getItems().add(item);
            }
            receivingRequests.save(receiving);
        }
        return view(fulfillment);
    }

    @Transactional
    public Map<String, Object> accept(UUID fulfillmentId, FulfillmentDtos.Decision input) {
        SalesStockFulfillment fulfillment = fulfillmentForUpdate(fulfillmentId);
        if (ACCEPTED.equals(fulfillment.getStatus())) return view(fulfillment);
        if (!CONFIRMED.equals(fulfillment.getStatus())) {
            throw new ConflictException("FULFILLMENT_NOT_PENDING", "Only confirmed fulfillments can be accepted");
        }
        SalesStockRequest request = requestForUpdate(fulfillment.getSalesStockRequestId());
        requireRequestOrganization(request);
        for (SalesStockFulfillmentLine line : fulfillment.getLines()) {
            InventoryBalance destination = balanceForUpdate(line.getProductId(), fulfillment.getDestinationZone());
            if (zero(destination.getReserved()).compareTo(line.getQuantity()) < 0) {
                throw new ConflictException("FULFILLMENT_PENDING_STOCK_MISSING", "Pending destination stock is no longer available");
            }
            destination.setReserved(zero(destination.getReserved()).subtract(line.getQuantity()));
            balances.save(destination);
            SalesStockRequestItem requestLine = requestItem(request, line.getProductId());
            decreasePending(requestLine, line.getQuantity());
            requestLine.setAcceptedQuantity(zero(requestLine.getAcceptedQuantity()).add(line.getQuantity()));
        }
        fulfillment.setStatus(ACCEPTED);
        fulfillment.setDecidedBy(actor.employeeId());
        fulfillment.setDecisionNotes(input.notes());
        request.setStatus(requestStatusAfterAcceptance(request));
        return view(fulfillment);
    }

    @Transactional
    public Map<String, Object> reject(UUID fulfillmentId, FulfillmentDtos.Decision input) {
        SalesStockFulfillment fulfillment = fulfillmentForUpdate(fulfillmentId);
        if (REJECTED.equals(fulfillment.getStatus())) return view(fulfillment);
        if (!CONFIRMED.equals(fulfillment.getStatus())) {
            throw new ConflictException("FULFILLMENT_NOT_PENDING", "Only confirmed fulfillments can be rejected");
        }
        SalesStockRequest request = requestForUpdate(fulfillment.getSalesStockRequestId());
        requireRequestOrganization(request);
        for (SalesStockFulfillmentLine line : fulfillment.getLines()) {
            decreasePending(requestItem(request, line.getProductId()), line.getQuantity());
        }
        fulfillment.setStatus(REJECTED);
        receivingRequests.findByFulfillmentId(fulfillmentId).ifPresent(receiving -> receiving.setStatus("REJECTED"));
        fulfillment.setDecidedBy(actor.employeeId());
        fulfillment.setDecisionNotes(input.notes());
        request.setStatus(CANCELLED);
        return view(fulfillment);
    }

    @Transactional
    public Map<String, Object> receivePhysicalReturn(UUID fulfillmentId, FulfillmentDtos.ReturnReceipt input) {
        SalesStockFulfillment fulfillment = fulfillmentForUpdate(fulfillmentId);
        if (RETURNED.equals(fulfillment.getStatus())) return view(fulfillment);
        if (!REJECTED.equals(fulfillment.getStatus())) {
            throw new ConflictException("FULFILLMENT_RETURN_NOT_ALLOWED", "Only rejected fulfillments can be physically returned");
        }
        SalesStockRequest request = requestForUpdate(fulfillment.getSalesStockRequestId());
        requireRequestOrganization(request);
        OrganizationZone warehouseZone = activeZone(input.warehouseZoneId());
        if (!warehouseZone.getSite().getId().equals(fulfillment.getWarehouseId())) {
            throw new ConflictException("FULFILLMENT_INVALID_RETURN_ZONE", "Return zone must belong to the request warehouse");
        }
        Map<UUID, BigDecimal> expected = fulfillmentQuantities(fulfillment.getLines());
        Map<UUID, BigDecimal> returned = returnQuantities(input.lines());
        if (!expected.equals(returned)) {
            throw new ConflictException("FULFILLMENT_RETURN_QUANTITY", "Physical return must contain each rejected fulfillment quantity exactly once");
        }
        for (SalesStockFulfillmentLine line : fulfillment.getLines()) {
            BigDecimal returnQuantity = line.getQuantity();
            InventoryBalance destination = balanceForUpdate(line.getProductId(), fulfillment.getDestinationZone());
            if (zero(destination.getOnHand()).compareTo(returnQuantity) < 0
                    || zero(destination.getReserved()).compareTo(returnQuantity) < 0) {
                throw new ConflictException("FULFILLMENT_RETURN_STOCK_MISSING", "Rejected stock is no longer pending physical return");
            }
            destination.setOnHand(zero(destination.getOnHand()).subtract(returnQuantity));
            destination.setReserved(zero(destination.getReserved()).subtract(returnQuantity));
            balances.save(destination);
            InventoryBalance warehouse = balanceForUpdate(line.getProductId(), warehouseZone);
            warehouse.setOnHand(zero(warehouse.getOnHand()).add(returnQuantity));
            balances.save(warehouse);
            movement(line.getProductId(), fulfillment.getDestinationZone(), warehouseZone, returnQuantity,
                    "SALES_FULFILLMENT_RETURN_RECEIVED", reference(fulfillment));
            SalesStockRequestItem requestLine = requestItem(request, line.getProductId());
            if (zero(requestLine.getFulfilledQuantity()).compareTo(returnQuantity) < 0) {
                throw new ConflictException("FULFILLMENT_RETURN_QUANTITY_MISMATCH", "Returned quantity exceeds the request's fulfilled quantity");
            }
            requestLine.setFulfilledQuantity(zero(requestLine.getFulfilledQuantity()).subtract(returnQuantity));
        }
        fulfillment.setStatus(RETURNED);
        return view(fulfillment);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(UUID requestId) {
        request(requestId);
        return fulfillments.findAllByOrganizationIdAndSalesStockRequestIdOrderByCreatedAtAsc(organizationId(), requestId)
                .stream().map(this::view).toList();
    }

    private void requireOpenRequest(SalesStockRequest request) {
        requireRequestOrganization(request);
        if (!REQUEST_ACCEPTED.equals(request.getStatus()) && !PARTIALLY_FULFILLED.equals(request.getStatus())) {
            throw new ConflictException("FULFILLMENT_REQUEST_NOT_OPEN", "Warehouse must accept the request before fulfillment");
        }
    }

    private void requireRequestOrganization(SalesStockRequest request) {
        if (request.getOrganization() == null || !organizationId().equals(request.getOrganization().getId())) {
            throw new ConflictException("FULFILLMENT_REQUEST_ORGANIZATION", "Sales stock request belongs to another organization");
        }
    }

    private String requestStatusAfterConfirmation(SalesStockRequest request) {
        boolean allRequestedAllocated = request.getItems().stream().allMatch(item ->
                zero(item.getFulfilledQuantity()).compareTo(item.getRequestedQuantity()) == 0);
        return allRequestedAllocated ? PENDING_ACCEPTANCE : PARTIALLY_FULFILLED;
    }

    private String requestStatusAfterAcceptance(SalesStockRequest request) {
        boolean allAccepted = request.getItems().stream().allMatch(item ->
                zero(item.getAcceptedQuantity()).compareTo(item.getRequestedQuantity()) == 0);
        if (allAccepted) return FULFILLED;
        boolean hasPending = request.getItems().stream().anyMatch(item -> zero(item.getPendingAcceptanceQuantity()).signum() > 0);
        return hasPending ? PENDING_ACCEPTANCE : PARTIALLY_FULFILLED;
    }

    private void decreasePending(SalesStockRequestItem item, BigDecimal quantity) {
        if (zero(item.getPendingAcceptanceQuantity()).compareTo(quantity) < 0) {
            throw new ConflictException("FULFILLMENT_PENDING_QUANTITY_MISMATCH", "Fulfillment exceeds the request's pending acceptance quantity");
        }
        item.setPendingAcceptanceQuantity(zero(item.getPendingAcceptanceQuantity()).subtract(quantity));
    }

    private InventoryBalance balanceForUpdate(UUID productId, OrganizationZone zone) {
        return balances.findForUpdate(organizationId(), productId, zone.getId()).orElseGet(() -> {
            InventoryBalance balance = new InventoryBalance();
            balance.setOrganization(organization());
            balance.setProductId(productId);
            balance.setZone(zone);
            balance.setOnHand(BigDecimal.ZERO);
            balance.setReserved(BigDecimal.ZERO);
            return balance;
        });
    }

    private OrganizationZone activeZone(UUID id) {
        return zones.findByIdAndSiteOrganizationIdAndActiveTrue(id, organizationId())
                .filter(zone -> zone.getSite().getStatus() == LocationStatus.ACTIVE)
                .orElseThrow(() -> new ConflictException("FULFILLMENT_INVALID_ZONE", "Zone must be active and belong to the authenticated organization"));
    }

    private OrganizationSite resolveOrganizationWarehouse() {
        List<OrganizationSite> warehouses = sites.findAllByOrganizationIdOrderByNameAsc(organizationId()).stream()
                .filter(candidate -> candidate.getType() == LocationType.WAREHOUSE).toList();
        if (warehouses.isEmpty()) {
            throw new ConflictException("FULFILLMENT_WAREHOUSE_MISSING", "Organization has no warehouse configured");
        }
        if (warehouses.size() != 1) {
            throw new ConflictException("FULFILLMENT_MULTIPLE_WAREHOUSES", "Organization must have exactly one warehouse");
        }
        OrganizationSite warehouse = warehouses.get(0);
        if (warehouse.getStatus() != LocationStatus.ACTIVE) {
            throw new ConflictException("FULFILLMENT_WAREHOUSE_INACTIVE", "Organization warehouse must be active");
        }
        return warehouse;
    }

    private OrganizationSite site(UUID id, LocationType type) {
        return sites.findByIdAndOrganizationId(id, organizationId())
                .filter(site -> site.getType() == type && site.getStatus() == LocationStatus.ACTIVE)
                .orElseThrow(() -> new ConflictException("FULFILLMENT_INVALID_SITE", "Request site must be active and belong to the authenticated organization"));
    }

    private void requireOrganizationProduct(UUID productId) {
        if (!organizationProducts.existsByOrganizationIdAndProductId(organizationId(), productId)) {
            throw new ConflictException("FULFILLMENT_INVALID_PRODUCT", "Product does not belong to the authenticated organization");
        }
    }

    private SalesStockRequest requestForUpdate(UUID id) {
        return requests.findForUpdateByIdAndOrganizationId(id, organizationId())
                .orElseThrow(() -> new NotFoundException("Sales stock request not found"));
    }

    private SalesStockRequest request(UUID id) {
        return requests.findByIdAndOrganizationId(id, organizationId())
                .orElseThrow(() -> new NotFoundException("Sales stock request not found"));
    }

    private SalesStockFulfillment fulfillmentForUpdate(UUID id) {
        return fulfillments.findForUpdateByIdAndOrganizationId(id, organizationId())
                .orElseThrow(() -> new NotFoundException("Sales fulfillment not found"));
    }

    private SalesStockRequestItem requestItem(SalesStockRequest request, UUID productId) {
        return request.getItems().stream().filter(item -> item.getProductId().equals(productId)).findFirst()
                .orElseThrow(() -> new ConflictException("FULFILLMENT_PRODUCT_NOT_REQUESTED", "Product is not on the Sales stock request"));
    }

    private Map<UUID, BigDecimal> fulfillmentQuantities(List<SalesStockFulfillmentLine> lines) {
        Map<UUID, BigDecimal> result = new HashMap<>();
        lines.forEach(line -> result.merge(line.getProductId(), line.getQuantity(), BigDecimal::add));
        return result;
    }

    private Map<UUID, BigDecimal> returnQuantities(List<FulfillmentDtos.ReturnLine> lines) {
        Map<UUID, BigDecimal> result = new HashMap<>();
        for (FulfillmentDtos.ReturnLine line : lines) {
            requireOrganizationProduct(line.productId());
            if (result.putIfAbsent(line.productId(), quantity(line.quantity())) != null) {
                throw new BadRequestException("FULFILLMENT_DUPLICATE_RETURN_PRODUCT", "A return product may appear only once");
            }
        }
        return result;
    }

    private void movement(UUID productId, OrganizationZone from, OrganizationZone to, BigDecimal quantity,
                          String type, String reference) {
        StockMovement movement = new StockMovement();
        movement.setOrganization(organization());
        movement.setProductId(productId);
        movement.setFromZone(from);
        movement.setToZone(to);
        movement.setQuantity(quantity);
        movement.setMovementType(type);
        movement.setReference(reference);
        movements.save(movement);
    }

    private Map<String, Object> view(SalesStockFulfillment fulfillment) {
        return Map.of("id", fulfillment.getId(), "salesStockRequestId", fulfillment.getSalesStockRequestId(),
                "warehouseId", fulfillment.getWarehouseId(), "salesHallId", fulfillment.getSalesHallId(),
                "destinationZoneId", fulfillment.getDestinationZone().getId(), "status", fulfillment.getStatus(),
                "lines", fulfillment.getLines().stream().map(line -> Map.of("productId", line.getProductId(),
                        "quantity", line.getQuantity(), "allocations", line.getAllocations().stream().map(allocation ->
                        Map.of("sourceZoneId", allocation.getSourceZone().getId(), "quantity", allocation.getQuantity())).toList())).toList());
    }

    private Map<String, Object> requestView(SalesStockRequest request) {
        return Map.of("id", request.getId(), "status", request.getStatus(),
                "items", request.getItems().stream().map(item ->
                        Map.of("productId", item.getProductId(), "requestedQuantity", item.getRequestedQuantity(),
                                "fulfilledQuantity", zero(item.getFulfilledQuantity()),
                                "pendingAcceptanceQuantity", zero(item.getPendingAcceptanceQuantity()),
                                "acceptedQuantity", zero(item.getAcceptedQuantity()))).toList());
    }

    private BigDecimal quantity(BigDecimal value) {
        if (value == null) throw new BadRequestException("FULFILLMENT_QUANTITY_REQUIRED", "Quantity is required");
        BigDecimal normalized = value.setScale(2, RoundingMode.HALF_UP);
        if (normalized.signum() <= 0) throw new BadRequestException("FULFILLMENT_INVALID_QUANTITY", "Quantity must be positive");
        return normalized;
    }

    private BigDecimal available(InventoryBalance balance) { return zero(balance.getOnHand()).subtract(zero(balance.getReserved())); }
    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private UUID organizationId() { return actor.organizationId(); }
    private Organization organization() { return organizations.findById(organizationId()).orElseThrow(() -> new NotFoundException("Organization not found")); }
    private String reference(SalesStockFulfillment fulfillment) { return "SALES_FULFILLMENT:" + fulfillment.getId(); }
}
