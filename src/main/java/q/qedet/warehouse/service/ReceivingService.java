package q.qedet.warehouse.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.BadRequestException;
import q.qedet.common.exception.ConflictException;
import q.qedet.common.exception.NotFoundException;
import q.qedet.employee.entity.Employee;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.procurement.entity.PurchaseOrder;
import q.qedet.procurement.entity.PurchaseOrderItem;
import q.qedet.procurement.entity.PurchaseRequestItem;
import q.qedet.procurement.repository.PurchaseOrderItemRepository;
import q.qedet.procurement.repository.PurchaseOrderRepository;
import q.qedet.product.dto.OrganizationProductRequest;
import q.qedet.product.entity.*;
import q.qedet.product.repository.OrganizationProductRepository;
import q.qedet.product.repository.ProductRepository;
import q.qedet.product.service.OrganizationProductService;
import q.qedet.common.model.LocationStatus;
import q.qedet.common.model.LocationType;
import q.qedet.organization.entity.OrganizationSite;
import q.qedet.organization.entity.OrganizationZone;
import q.qedet.organization.repository.OrganizationSiteRepository;
import q.qedet.organization.repository.OrganizationZoneRepository;
import q.qedet.qr.dto.QrCodeResponse;
import q.qedet.qr.entity.QrCodeType;
import q.qedet.qr.entity.QrTargetKind;
import q.qedet.qr.service.QrCodeService;
import q.qedet.warehouse.dto.ReceivingDtos;
import q.qedet.warehouse.entity.*;
import q.qedet.warehouse.repository.*;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.*;

@Service
public class ReceivingService {
    private final ActorContext actor;
    private final EmployeeRepository employees;
    private final ReceivingRequestRepository requests;
    private final ReceivingRequestItemRepository requestItems;
    private final ReceivingOperationRepository operations;
    private final ReceivingOperationItemRepository operationItems;
    private final ReceivingOperationSerialRepository serials;
    private final ReceivingOperationZoneAssignmentRepository assignments;
    private final WarehouseService warehouseService;
    private final QrCodeService qrCodes;
    private final ProductRepository products;
    private final OrganizationProductRepository organizationProducts;
    private final OrganizationProductService organizationProductService;
    private final OrganizationSiteRepository sites;
    private final OrganizationZoneRepository organizationZones;

    @Autowired
    public ReceivingService(ActorContext actor, EmployeeRepository employees,
                            PurchaseOrderRepository ignoredPurchaseOrders,
                            PurchaseOrderItemRepository ignoredPurchaseOrderItems,
                            ReceivingRequestRepository requests, ReceivingRequestItemRepository requestItems,
                            ReceivingOperationRepository operations, ReceivingOperationItemRepository operationItems,
                            ReceivingOperationSerialRepository serials,
                            ReceivingOperationZoneAssignmentRepository assignments,
                            WarehouseService warehouseService, QrCodeService qrCodes,
                            ProductRepository products, OrganizationProductRepository organizationProducts,
                            OrganizationProductService organizationProductService,
                            OrganizationSiteRepository sites, OrganizationZoneRepository organizationZones) {
        this.actor = actor;
        this.employees = employees;
        this.requests = requests;
        this.requestItems = requestItems;
        this.operations = operations;
        this.operationItems = operationItems;
        this.serials = serials;
        this.assignments = assignments;
        this.warehouseService = warehouseService;
        this.qrCodes = qrCodes;
        this.products = products;
        this.organizationProducts = organizationProducts;
        this.organizationProductService = organizationProductService;
        this.sites = sites;
        this.organizationZones = organizationZones;
    }


    @Transactional
    public void reconcileReadyItem(PurchaseOrder order, PurchaseOrderItem item) {
        if (item.getPurchaseRequestItem() == null) {
            throw new BadRequestException("Receiving requires a purchase order item linked to a purchase request item");
        }
        ReceivingRequest request = requests.findAllByPurchaseOrderId(order.getId()).stream().findFirst()
                .orElseGet(() -> createRequest(order));
        if (requestItems.findByReceivingRequestIdAndPurchaseOrderItemId(request.getId(), item.getId()).isEmpty()) {
            ReceivingRequestItem line = new ReceivingRequestItem();
            line.setReceivingRequest(request);
            line.setPurchaseOrderItem(item);
            line.setExpectedQuantity(zero(item.getQuantity()));
            requestItems.save(line);
        }
    }

    @Transactional
    public ReceivingDtos.OperationResponse start(UUID requestId, ReceivingDtos.StartOperationRequest body) {
        UUID organizationId = actor.organizationId();
        ReceivingRequest request = requests.findByIdAndOrganizationId(requestId, organizationId)
                .orElseThrow(() -> new NotFoundException("RECEIVING_REQUEST_NOT_FOUND"));
        if (request.getStatus() == ReceivingRequest.Status.COMPLETED || request.getStatus() == ReceivingRequest.Status.CANCELLED) {
            throw new ConflictException("RECEIVING_REQUEST_NOT_EDITABLE", "Receiving request cannot be started");
        }
        Optional<ReceivingOperation> active = operations.findFirstByReceivingRequestIdAndStatusOrderByStartedAtDesc(
                requestId, ReceivingOperation.Status.IN_PROGRESS);
        if (active.isPresent()) return response(active.get());

        warehouseSite(request, body == null ? null : body.warehouseId());

        Employee employee = employees.findByIdAndOrganizationId(actor.employeeId(), organizationId)
                .orElseThrow(() -> new NotFoundException("WAREHOUSE_EMPLOYEE_NOT_FOUND"));
        ReceivingOperation operation = new ReceivingOperation();
        operation.setOrganization(request.getOrganization());
        operation.setReceivingRequest(request);
        operation.setWarehouseEmployee(employee);
        operation.setStartedAt(Instant.now());
        operation.setNotes(body == null ? null : body.notes());
        operation.setStatus(ReceivingOperation.Status.IN_PROGRESS);
        request.setStatus(ReceivingRequest.Status.IN_PROGRESS);
        requests.save(request);
        return response(operations.save(operation));
    }

    @Transactional
    public ReceivingDtos.OperationResponse record(UUID operationId, ReceivingDtos.OperationItemRequest input) {
        ReceivingOperation operation = ownedOperation(operationId);
        ensureEditable(operation);
        ReceivingRequestItem requestItem = operationItem(operation, input.receivingRequestItemId());
        validateQuantities(input);

        ReceivingOperationItem line = findOrCreateLine(operation, requestItem);
        requireIdentification(line, input);
        ensureNotOverReceived(operation, requestItem, input.receivedQuantity());
        validateSerials(line.getProduct(), input);

        line.setReceivedQuantity(input.receivedQuantity());
        line.setAcceptedQuantity(input.acceptedQuantity());
        line.setRejectedQuantity(input.rejectedQuantity());
        line.setNotes(input.notes());
        if (input.supplierQrIdentifier() != null) line.setSupplierQrIdentifier(input.supplierQrIdentifier());
        line = operationItems.save(line);
        replaceSerials(line, line.getProduct(), input.serialIdentities());
        replaceZoneAssignments(line, input.zoneAssignments(), input.acceptedQuantity());
        recalculateRequestStatus(operation.getReceivingRequest());
        return response(operation);
    }

    @Transactional
    public ReceivingDtos.SupplierQrResolveResponse resolveSupplierQr(UUID operationId,
                                                                       ReceivingDtos.SupplierQrResolveRequest input) {
        ReceivingOperation operation = ownedOperation(operationId);
        ensureEditable(operation);
        ReceivingRequestItem requestItem = operationItem(operation, input.receivingRequestItemId());
        QrCodeResponse existing = findQr(input.identifier());
        if (existing == null) {
            prepareLine(operation, requestItem, null, input.identifier(),
                    ReceivingOperationItem.ProductResolution.CREATE_WITH_SUPPLIER_QR);
            return pendingQr(requestItem, input.identifier(), input.serialNumber());
        }
        Product product = productForQr(existing);
        return matchedQr(requestItem, input.identifier(), product, input.serialNumber(), existing, true);
    }

    @Transactional
    public ReceivingDtos.SupplierQrResolveResponse decideSupplierQr(UUID operationId,
                                                                      ReceivingDtos.SupplierQrDecisionRequest input) {
        ReceivingOperation operation = ownedOperation(operationId);
        ensureEditable(operation);
        ReceivingRequestItem requestItem = operationItem(operation, input.receivingRequestItemId());
        if (qrCodes == null) throw new ConflictException("RECEIVING_QR_UNAVAILABLE", "QR resolution is unavailable");

        QrCodeResponse existing = qrCodes.resolveInOrganization(actor.organizationId(), input.identifier());
        Product product = productForQr(existing);
        if (input.decision() == ReceivingDtos.SupplierQrDecision.LINK_EXISTING_PRODUCT) {
            prepareLine(operation, requestItem, product, input.identifier(),
                    ReceivingOperationItem.ProductResolution.LINKED_EXISTING_PRODUCT);
            return matchedQr(requestItem, input.identifier(), product, null, existing, false);
        }
        prepareLine(operation, requestItem, null, null,
                ReceivingOperationItem.ProductResolution.CREATE_WITH_NEW_QEDET_QR);
        return pendingQr(requestItem, input.identifier(), null);
    }

    @Transactional
    public ReceivingDtos.OperationResponse complete(UUID operationId) {
        ReceivingOperation operation = ownedOperationForCompletion(operationId);
        if (operation.getStatus() == ReceivingOperation.Status.COMPLETED) return response(operation);
        ensureEditable(operation);
        if (operation.getItems().isEmpty()) {
            throw new ConflictException("RECEIVING_EMPTY_OPERATION", "At least one received item is required");
        }
        warehouseSite(operation.getReceivingRequest(), null);
        for (ReceivingOperationItem line : operation.getItems()) validateAssignmentsForCompletion(line);
        for (ReceivingOperationItem line : operation.getItems()) completeLine(line);

        operation.setStatus(ReceivingOperation.Status.COMPLETED);
        operation.setCompletedAt(Instant.now());
        warehouseService.applyCompletedReceivingOperation(operation);
        ReceivingOperation saved = operations.save(operation);
        recalculateRequestStatus(operation.getReceivingRequest());
        return response(saved);
    }

    @Transactional
    public ReceivingDtos.OperationResponse cancel(UUID operationId) {
        ReceivingOperation operation = ownedOperation(operationId);
        ensureEditable(operation);
        operation.setStatus(ReceivingOperation.Status.CANCELLED);
        ReceivingOperation saved = operations.save(operation);
        recalculateRequestStatus(operation.getReceivingRequest());
        return response(saved);
    }

    @Transactional(readOnly = true)
    public List<ReceivingDtos.RequestResponse> list(ReceivingRequest.Status status, UUID warehouseId, String purchaseOrderNumber) {
        return requestResponses(requests.findListRows(actor.organizationId(),
                status == null ? null : status.name(), warehouseId, purchaseOrderNumber, null));
    }

    @Transactional(readOnly = true)
    public ReceivingDtos.RequestResponse get(UUID id) {
        // The work-list projection has joins for display fields.  A legitimate
        // request must remain addressable even while a legacy procurement row is
        // incomplete, so identity and tenant isolation come from the entity lookup.
        ReceivingRequest request = requests.findByIdAndOrganizationId(id, actor.organizationId())
                .orElseThrow(() -> new NotFoundException("RECEIVING_REQUEST_NOT_FOUND"));
        List<ReceivingDtos.RequestResponse> projected = requestResponses(
                requests.findListRows(actor.organizationId(), null, null, null, id));
        return projected.isEmpty() ? requestResponse(request) : projected.get(0);
    }

    @Transactional(readOnly = true)
    public ReceivingDtos.OperationResponse getOperation(UUID id) { return response(ownedOperation(id)); }
    public ReceivingDtos.HistoryResponse history(UUID id) { return new ReceivingDtos.HistoryResponse(List.of()); }
    public List<ReceivingDtos.OperationResponse> recoverUnappliedCompletedOperations() { return List.of(); }

    private ReceivingRequest createRequest(PurchaseOrder order) {
        ReceivingRequest request = new ReceivingRequest();
        request.setOrganization(order.getOrganization());
        request.setPurchaseOrder(order);
        request.setRequestNumber("RR-" + order.getNumber());
        return requests.save(request);
    }

    private ReceivingOperation ownedOperation(UUID id) {
        return operations.findByIdAndOrganizationId(id, actor.organizationId())
                .orElseThrow(() -> new NotFoundException("RECEIVING_OPERATION_NOT_FOUND"));
    }

    private ReceivingOperation ownedOperationForCompletion(UUID id) {
        return operations.findWithLockByIdAndOrganizationId(id, actor.organizationId())
                .orElseThrow(() -> new NotFoundException("RECEIVING_OPERATION_NOT_FOUND"));
    }

    private ReceivingRequestItem operationItem(ReceivingOperation operation, UUID id) {
        ReceivingRequestItem item = requestItems.findByIdAndReceivingRequestOrganizationId(id, actor.organizationId())
                .orElseThrow(() -> new NotFoundException("RECEIVING_REQUEST_ITEM_NOT_FOUND"));
        if (!item.getReceivingRequest().getId().equals(operation.getReceivingRequest().getId())) {
            throw new NotFoundException("RECEIVING_REQUEST_ITEM_NOT_FOUND");
        }
        return item;
    }

    private void ensureEditable(ReceivingOperation operation) {
        if (operation.getStatus() != ReceivingOperation.Status.IN_PROGRESS) {
            throw new ConflictException("RECEIVING_OPERATION_NOT_EDITABLE", "Receiving operation is not editable");
        }
    }

    private ReceivingOperationItem findOrCreateLine(ReceivingOperation operation, ReceivingRequestItem requestItem) {
        return operationItems.findByReceivingOperationIdAndReceivingRequestItemId(operation.getId(), requestItem.getId())
                .orElseGet(() -> { ReceivingOperationItem line = new ReceivingOperationItem();
                    line.setReceivingOperation(operation); line.setReceivingRequestItem(requestItem); operation.getItems().add(line); return line; });
    }

    private void validateQuantities(ReceivingDtos.OperationItemRequest input) {
        if (input.receivedQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("RECEIVING_INVALID_QUANTITY", "receivedQuantity must be greater than zero");
        }
        if (input.acceptedQuantity().add(input.rejectedQuantity()).compareTo(input.receivedQuantity()) != 0) {
            throw new BadRequestException("RECEIVING_QUANTITY_MISMATCH", "Accepted and rejected quantities must equal received quantity");
        }
    }

    /** Resolve and retain the canonical destination site for this request. */
    private OrganizationSite warehouseSite(ReceivingRequest request, UUID requestedSiteId) {
        UUID organizationId = actor.organizationId();
        if (sites == null || organizationZones == null) {
            throw new ConflictException("RECEIVING_SITE_TOPOLOGY_UNAVAILABLE", "Receiving site topology is unavailable");
        }
        UUID existingSiteId = request.getDestinationLocation() == null ? null : request.getDestinationLocation().getId();
        if (existingSiteId != null && requestedSiteId != null && !existingSiteId.equals(requestedSiteId)) {
            throw new ConflictException("RECEIVING_WAREHOUSE_SITE_MISMATCH", "Receiving request already has a different Warehouse site");
        }
        UUID siteId = existingSiteId == null ? requestedSiteId : existingSiteId;
        OrganizationSite site;
        if (siteId != null) {
            site = sites.findByIdAndOrganizationId(siteId, organizationId)
                    .filter(candidate -> candidate.getType() == LocationType.WAREHOUSE && candidate.getStatus() == LocationStatus.ACTIVE)
                    .orElseThrow(() -> new ConflictException("RECEIVING_WAREHOUSE_SITE_REQUIRED", "An active Warehouse site in this organization is required for receiving"));
        } else {
            List<OrganizationSite> warehouses = sites.findAllByOrganizationIdAndStatusOrderByNameAsc(organizationId, LocationStatus.ACTIVE)
                    .stream().filter(candidate -> candidate.getType() == LocationType.WAREHOUSE).toList();
            site = warehouses.size() == 1 ? warehouses.get(0) : warehouses.stream().filter(OrganizationSite::isDefault).reduce((first, second) -> null).orElse(null);
            if (site == null) throw new ConflictException("RECEIVING_WAREHOUSE_SITE_REQUIRED", "Select an active Warehouse site for receiving");
            request.setDestinationLocation(site);
        }
        return site;
    }

    void replaceZoneAssignments(ReceivingOperationItem line, List<ReceivingDtos.ZoneAssignmentRequest> input,
                                BigDecimal acceptedQuantity) {
        if (acceptedQuantity.signum() == 0) {
            if (input != null && !input.isEmpty()) throw new BadRequestException("RECEIVING_ZERO_ACCEPTED_ASSIGNMENT", "Zero accepted quantity cannot have zone assignments");
            deleteAssignmentsAndFlush(line.getId());
            return;
        }
        if (input == null || input.isEmpty()) {
            throw new BadRequestException("RECEIVING_ZONE_ASSIGNMENT_REQUIRED", "Accepted quantity requires destination zone assignments");
        }
        Map<UUID, BigDecimal> quantities = new LinkedHashMap<>();
        for (ReceivingDtos.ZoneAssignmentRequest request : input) {
            if (request.quantity() == null || request.quantity().signum() <= 0)
                throw new BadRequestException("RECEIVING_INVALID_ZONE_QUANTITY", "Zone assignment quantity must be greater than zero");
            if (quantities.putIfAbsent(request.zoneId(), request.quantity()) != null)
                throw new BadRequestException("RECEIVING_DUPLICATE_ZONE_ASSIGNMENT", "A destination zone may be assigned once per receiving item");
        }
        BigDecimal assigned = quantities.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (assigned.compareTo(acceptedQuantity) != 0)
            throw new BadRequestException("RECEIVING_ZONE_ASSIGNMENT_MISMATCH", "Zone assignment quantities must equal accepted quantity");
        OrganizationSite site = warehouseSite(line.getReceivingOperation().getReceivingRequest(), null);
        Map<OrganizationZone, BigDecimal> validatedAssignments = new LinkedHashMap<>();
        quantities.forEach((zoneId, quantity) -> {
            OrganizationZone zone = organizationZones.findByIdAndSiteId(zoneId, site.getId())
                    .filter(OrganizationZone::isActive)
                    .orElseThrow(() -> new BadRequestException("RECEIVING_INVALID_DESTINATION_ZONE", "Destination zone must be active and belong to the organization's Warehouse site"));
            if (zone.getSite() == null || zone.getSite().getOrganization() == null
                    || !actor.organizationId().equals(zone.getSite().getOrganization().getId())) {
                throw new BadRequestException("RECEIVING_INVALID_DESTINATION_ZONE", "Destination zone must belong to the authenticated organization");
            }
            validatedAssignments.put(zone, quantity);
        });
        deleteAssignmentsAndFlush(line.getId());
        validatedAssignments.forEach((zone, quantity) -> {
            ReceivingOperationZoneAssignment assignment = new ReceivingOperationZoneAssignment();
            assignment.setReceivingOperationItem(line); assignment.setZone(zone); assignment.setAcceptedQuantity(quantity);
            assignments.save(assignment);
        });
    }

    private void deleteAssignmentsAndFlush(UUID operationItemId) {
        assignments.deleteAllByReceivingOperationItemIdInBulk(operationItemId);
        assignments.flush();
    }

    private void validateAssignmentsForCompletion(ReceivingOperationItem line) {
        BigDecimal accepted = zero(line.getAcceptedQuantity());
        List<ReceivingOperationZoneAssignment> saved = assignments.findAllByReceivingOperationItemIdOrderByCreatedAtAsc(line.getId());
        if (accepted.signum() == 0) {
            if (!saved.isEmpty()) throw new ConflictException("RECEIVING_ZERO_ACCEPTED_ASSIGNMENT", "Rejected-only lines cannot have destination assignments");
            return;
        }
        if (saved.isEmpty()) throw new ConflictException("RECEIVING_ZONE_ASSIGNMENT_REQUIRED", "Accepted quantity requires destination zone assignments");
        OrganizationSite site = warehouseSite(line.getReceivingOperation().getReceivingRequest(), null);
        Set<UUID> uniqueZones = new HashSet<>(); BigDecimal total = BigDecimal.ZERO;
        for (ReceivingOperationZoneAssignment assignment : saved) {
            OrganizationZone zone = assignment.getZone();
            if (zone == null || !uniqueZones.add(zone.getId()) || !zone.isActive() || !zone.getSite().getId().equals(site.getId()) ||
                    !zone.getSite().getOrganization().getId().equals(actor.organizationId()))
                throw new ConflictException("RECEIVING_INVALID_DESTINATION_ZONE", "Receiving assignment has an invalid destination zone");
            if (zero(assignment.getAcceptedQuantity()).signum() <= 0)
                throw new ConflictException("RECEIVING_INVALID_ZONE_QUANTITY", "Receiving assignment quantity must be greater than zero");
            total = total.add(assignment.getAcceptedQuantity());
        }
        if (total.compareTo(accepted) != 0)
            throw new ConflictException("RECEIVING_ZONE_ASSIGNMENT_MISMATCH", "Zone assignment quantities must equal accepted quantity");
    }

    private void requireIdentification(ReceivingOperationItem line, ReceivingDtos.OperationItemRequest input) {
        boolean pending = line.getProduct() == null
                && line.getProductResolution() == ReceivingOperationItem.ProductResolution.PENDING_PRODUCT;
        if (pending && line.getSupplierQrIdentifier() == null && input.supplierQrIdentifier() == null) {
            throw new ConflictException("RECEIVING_IDENTIFICATION_REQUIRED", "Scan a supplier QR before recording a pending-product line");
        }
    }

    private void ensureNotOverReceived(ReceivingOperation operation, ReceivingRequestItem item, BigDecimal quantity) {
        BigDecimal previous = zero(operationItems.receivedQuantityForOtherOperations(item.getId(), operation.getId()));
        if (previous.add(quantity).compareTo(item.getExpectedQuantity()) > 0) {
            throw new ConflictException("RECEIVING_OVERRECEIPT", "Received quantity exceeds the request remaining quantity");
        }
    }

    /**
     * Derives the request state from persisted work. Active operations take precedence
     * over quantities; cancelled operations are intentionally excluded from totals.
     */
    void recalculateRequestStatus(ReceivingRequest request) {
        if (request.getStatus() == ReceivingRequest.Status.CANCELLED
                || request.getStatus() == ReceivingRequest.Status.COMPLETED) {
            return;
        }
        if (operations.existsByReceivingRequestIdAndStatus(request.getId(), ReceivingOperation.Status.IN_PROGRESS)) {
            request.setStatus(ReceivingRequest.Status.IN_PROGRESS);
            requests.save(request);
            return;
        }

        List<ReceivingRequestItem> items = requestItems.findAllByReceivingRequestId(request.getId());
        boolean hasExpectedQuantity = false;
        boolean hasReceivedQuantity = false;
        boolean allExpectedQuantitiesReceived = true;
        for (ReceivingRequestItem item : items) {
            BigDecimal expected = zero(item.getExpectedQuantity());
            BigDecimal received = zero(operationItems.receivedQuantityForRequestItem(item.getId()));
            if (received.signum() > 0) hasReceivedQuantity = true;
            if (expected.signum() > 0) {
                hasExpectedQuantity = true;
                if (received.compareTo(expected) < 0) allExpectedQuantitiesReceived = false;
            }
        }

        if (hasExpectedQuantity && allExpectedQuantitiesReceived) {
            request.setStatus(ReceivingRequest.Status.COMPLETED);
        } else if (hasReceivedQuantity) {
            request.setStatus(ReceivingRequest.Status.PARTIALLY_RECEIVED);
        } else {
            request.setStatus(ReceivingRequest.Status.PENDING);
        }
        requests.save(request);
    }

    private void validateSerials(Product product, ReceivingDtos.OperationItemRequest input) {
        List<ReceivingDtos.SerialIdentityRequest> identities = input.serialIdentities();
        if (product == null) return;
        if (product.getProductType() != ProductType.SERIALIZED) {
            if (identities != null && !identities.isEmpty()) throw new BadRequestException("RECEIVING_SERIALS_NOT_ALLOWED", "Serial identities are only valid for serialized products");
            return;
        }
        if (identities == null || identities.size() != input.receivedQuantity().intValueExact()
                || identities.stream().map(ReceivingDtos.SerialIdentityRequest::serialNumber).distinct().count() != identities.size()) {
            throw new BadRequestException("RECEIVING_SERIALS_REQUIRED", "Serialized items require one unique serial identity per received unit");
        }
    }

    private void replaceSerials(ReceivingOperationItem line, Product product, List<ReceivingDtos.SerialIdentityRequest> identities) {
        serials.deleteByReceivingOperationItemId(line.getId());
        if (product == null || identities == null) return;
        for (ReceivingDtos.SerialIdentityRequest identity : identities) {
            ReceivingOperationSerial serial = new ReceivingOperationSerial();
            serial.setReceivingOperationItem(line); serial.setProduct(product); serial.setSerialNumber(identity.serialNumber());
            serial.setSupplierQrIdentifier(identity.supplierQrIdentifier()); serial.setAccepted(identity.accepted());
            serials.save(serial);
        }
    }

    private QrCodeResponse findQr(String identifier) {
        if (qrCodes == null) {
            return null;
        }

        return qrCodes.findInOrganizationForReceiving(
                actor.organizationId(),
                identifier
        ).orElse(null);
    }

    private Product productForQr(QrCodeResponse qr) {
        if (qr.type() != QrCodeType.PRODUCT || qr.targetKind() != QrTargetKind.PRODUCT) {
            throw new ConflictException("QR_IDENTIFIER_ALREADY_ASSIGNED", "QR identifier is assigned to a different target");
        }
        return organizationProduct(qr.referenceId());
    }

    private void completeLine(ReceivingOperationItem line) {
        if (line.getAcceptedQuantity().compareTo(BigDecimal.ZERO) <= 0) return;
        Product product = line.getProduct();
        if (product == null) { product = registerPendingProduct(line); line.setProduct(product); operationItems.save(line); }
        if (qrCodes == null) return;
        if (product.getProductType() == ProductType.SERIALIZED) {
            serials.findAllByReceivingOperationItemId(line.getId()).stream().filter(ReceivingOperationSerial::isAccepted)
                    .forEach(serial -> qrCodes.resolveOrCreateReceivingSerial(actor.organizationId(), serial));
        } else if (line.getProductResolution() == ReceivingOperationItem.ProductResolution.CREATE_WITH_NEW_QEDET_QR) {
            qrCodes.createQrCode(QrCodeType.PRODUCT, actor.organizationId(), product.getId(), null);
        } else {
            qrCodes.resolveOrCreate(QrCodeType.PRODUCT, actor.organizationId(), product.getId(), QrTargetKind.PRODUCT, line.getSupplierQrIdentifier());
        }
    }

    private Product registerPendingProduct(ReceivingOperationItem line) {
        if (organizationProductService == null) throw new ConflictException("RECEIVING_PRODUCT_REGISTRATION_UNAVAILABLE", "Product registration is unavailable");
        PurchaseRequestItem source = line.getReceivingRequestItem().getPurchaseOrderItem().getPurchaseRequestItem();
        if (source.getProductName() == null || source.getProductName().isBlank()) {
            throw new ConflictException("RECEIVING_PRODUCT_DETAILS_REQUIRED", "Purchase item product name is required for product registration");
        }
        boolean supplierQr = line.getProductResolution() != ReceivingOperationItem.ProductResolution.CREATE_WITH_NEW_QEDET_QR;
        if (supplierQr && (line.getSupplierQrIdentifier() == null || line.getSupplierQrIdentifier().isBlank())) {
            throw new ConflictException("RECEIVING_IDENTIFICATION_REQUIRED", "A supplier QR is required before creating a product at completion");
        }
        OrganizationProductRequest request = new OrganizationProductRequest(null, source.getSku(), supplierQr ? line.getSupplierQrIdentifier() : null,
                source.getProductName(), source.getDescription(), null, null, null, productType(line.getReceivingRequestItem().getPurchaseOrderItem()),
                ProductStatus.ACTIVE, supplierQr ? ProductBarcodeType.QR : null, OrganizationProductStatus.ACTIVE);
        UUID productId = supplierQr ? organizationProductService.create(actor.organizationId(), request).productId()
                : organizationProductService.createForReceiving(actor.organizationId(), request).productId();
        return organizationProduct(productId);
    }

    private ProductType productType(PurchaseOrderItem item) {
        return switch (item.getProductType()) {
            case SERIALIZED -> ProductType.SERIALIZED;
            case WEIGHTED_BULK -> ProductType.WEIGHTED;
            case MULTI_PACK -> ProductType.BUNDLE;
            default -> ProductType.STANDARD;
        };
    }

    private ReceivingOperationItem prepareLine(ReceivingOperation operation, ReceivingRequestItem item, Product product,
                                               String supplierQr, ReceivingOperationItem.ProductResolution resolution) {
        ReceivingOperationItem line = findOrCreateLine(operation, item);
        if (product != null) line.setProduct(product);
        line.setSupplierQrIdentifier(supplierQr); line.setProductResolution(resolution);
        return operationItems.save(line);
    }

    private Product organizationProduct(UUID productId) {
        if (products == null || organizationProducts == null || !organizationProducts.existsByOrganizationIdAndProductId(actor.organizationId(), productId)) {
            throw new NotFoundException("PRODUCT_NOT_FOUND");
        }
        return products.findById(productId).orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND"));
    }

    private ReceivingDtos.SupplierQrResolveResponse pendingQr(ReceivingRequestItem item, String identifier, String serial) {
        PurchaseRequestItem source = item.getPurchaseOrderItem().getPurchaseRequestItem();
        return new ReceivingDtos.SupplierQrResolveResponse("VALID_QR_CODE", identifier, item.getId(), null, null, null, serial,
                null, null, null, source.getProductName(), source.getSku(), item.getPurchaseOrderItem().getProductType().name(), source.getUnit(), false);
    }

    private ReceivingDtos.SupplierQrResolveResponse matchedQr(ReceivingRequestItem item, String identifier, Product product,
                                                               String serial, QrCodeResponse qr, boolean decisionRequired) {
        PurchaseRequestItem source = item.getPurchaseOrderItem().getPurchaseRequestItem();
        return new ReceivingDtos.SupplierQrResolveResponse("MATCHES_PRODUCT", identifier, item.getId(), product.getId(), product.getName(),
                product.getProductType().name(), serial, qr.id(), qr.referenceId(), qr.targetKind().name(), source.getProductName(), source.getSku(),
                item.getPurchaseOrderItem().getProductType().name(), source.getUnit(), decisionRequired);
    }

    private List<ReceivingDtos.RequestResponse> requestResponses(List<ReceivingRequestRepository.ReceivingRequestListRow> rows) {
        Map<UUID, List<ReceivingRequestRepository.ReceivingRequestListRow>> grouped = new LinkedHashMap<>();
        for (var row : rows) grouped.computeIfAbsent(uuid(row.getRequestId()), ignored -> new ArrayList<>()).add(row);
        return grouped.values().stream().map(group -> {
            var first = group.get(0);
            List<ReceivingDtos.RequestItemResponse> items = group.stream().map(this::requestItemResponse).toList();
            return new ReceivingDtos.RequestResponse(uuid(first.getRequestId()), first.getRequestNumber(), uuid(first.getPurchaseOrderId()),
                    first.getPurchaseOrderNumber(), uuid(first.getWarehouseId()), uuid(first.getDeliveryLocationId()), first.getRequestStatus(), items);
        }).toList();
    }

    private ReceivingDtos.RequestItemResponse requestItemResponse(ReceivingRequestRepository.ReceivingRequestListRow row) {
        BigDecimal expected = zero(row.getExpectedQuantity()), received = zero(row.getReceivedQuantity());
        return new ReceivingDtos.RequestItemResponse(uuid(row.getRequestItemId()), uuid(row.getPurchaseOrderItemId()), uuid(row.getProductId()),
                row.getProductName(), row.getSku(), row.getProductType(), row.getBaseUnit(), expected, BigDecimal.ZERO,
                zero(row.getAcceptedQuantity()), zero(row.getRejectedQuantity()), received, expected.subtract(received).max(BigDecimal.ZERO));
    }

    private ReceivingDtos.RequestResponse requestResponse(ReceivingRequest request) {
        List<ReceivingDtos.RequestItemResponse> items = request.getItems().stream().map(item -> {
            PurchaseOrderItem orderItem = item.getPurchaseOrderItem();
            PurchaseRequestItem source = orderItem.getPurchaseRequestItem();
            Product product = source == null ? null : source.getProduct();
            BigDecimal received = BigDecimal.ZERO, accepted = BigDecimal.ZERO, rejected = BigDecimal.ZERO;
            for (ReceivingOperationItem operationItem : operationItems.findAllByReceivingRequestItemIdOrderByReceivingOperationStartedAtAsc(item.getId())) {
                received = received.add(zero(operationItem.getReceivedQuantity()));
                accepted = accepted.add(zero(operationItem.getAcceptedQuantity()));
                rejected = rejected.add(zero(operationItem.getRejectedQuantity()));
            }
            BigDecimal expected = zero(item.getExpectedQuantity());
            return new ReceivingDtos.RequestItemResponse(item.getId(), orderItem.getId(), product == null ? null : product.getId(),
                    product == null ? (source == null ? null : source.getProductName()) : product.getName(), source == null ? null : source.getSku(),
                    orderItem.getProductType().name(), source == null ? null : source.getUnit(), expected, BigDecimal.ZERO,
                    accepted, rejected, received, expected.subtract(received).max(BigDecimal.ZERO));
        }).toList();
        return new ReceivingDtos.RequestResponse(request.getId(), request.getRequestNumber(), request.getPurchaseOrder().getId(),
                request.getPurchaseOrder().getNumber(), null,
                request.getDestinationLocation() == null ? null : request.getDestinationLocation().getId(), request.getStatus().name(), items);
    }

    private ReceivingDtos.OperationResponse response(ReceivingOperation operation) {
        List<ReceivingDtos.OperationItemResponse> items = operation.getItems().stream().map(this::operationItemResponse).toList();
        Employee employee = operation.getWarehouseEmployee();
        return new ReceivingDtos.OperationResponse(operation.getId(), operation.getReceivingRequest().getId(), employee.getId(),
                employee.getFirstName() + " " + employee.getLastName(), operation.getStatus().name(), operation.getStartedAt(),
                operation.getCompletedAt(), operation.getNotes(), items);
    }

    private ReceivingDtos.OperationItemResponse operationItemResponse(ReceivingOperationItem item) {
        List<ReceivingDtos.SerialIdentityRequest> serialsResponse = serials.findAllByReceivingOperationItemId(item.getId()).stream()
                .map(serial -> new ReceivingDtos.SerialIdentityRequest(serial.getSerialNumber(), serial.getSupplierQrIdentifier(), serial.isAccepted())).toList();
        List<ReceivingDtos.ZoneAssignmentResponse> zones = assignments.findAllByReceivingOperationItemIdOrderByCreatedAtAsc(item.getId()).stream()
                .map(a -> new ReceivingDtos.ZoneAssignmentResponse(a.getZone().getId(), a.getAcceptedQuantity())).toList();
        return new ReceivingDtos.OperationItemResponse(item.getId(), item.getReceivingRequestItem().getId(), zero(item.getReceivedQuantity()),
                zero(item.getAcceptedQuantity()), zero(item.getRejectedQuantity()), item.getNotes(), item.getSupplierQrIdentifier(), serialsResponse, zones);
    }

    private UUID uuid(Object value) {
        if (value == null) return null;
        if (value instanceof UUID id) return id;
        if (value instanceof byte[] bytes) {
            if (bytes.length != 16) throw new IllegalArgumentException("Expected a BINARY(16) UUID but received " + bytes.length + " bytes");
            ByteBuffer buffer = ByteBuffer.wrap(bytes); return new UUID(buffer.getLong(), buffer.getLong());
        }
        if (value instanceof String text) return UUID.fromString(text);
        throw new IllegalArgumentException("Unsupported receiving projection UUID value: " + value.getClass().getName());
    }

    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
