package q.qedet.procurement.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.BadRequestException;
import q.qedet.common.exception.ConflictException;
import q.qedet.common.exception.ForbiddenException;
import q.qedet.common.exception.NotFoundException;
import q.qedet.common.model.Permission;
import q.qedet.employee.entity.Employee;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.repository.OrganizationRepository;
import q.qedet.procurement.dto.ProcurementDtos.PurchaseOrderItemRequest;
import q.qedet.procurement.dto.ProcurementDtos.ProductRequest;
import q.qedet.procurement.dto.ProcurementDtos.PurchaseOrderRequest;
import q.qedet.procurement.dto.ProcurementDtos.PurchaseRequestRequest;
import q.qedet.procurement.dto.ProcurementDtos.PurchaseRequestUpdateRequest;
import q.qedet.procurement.dto.ProcurementDtos.RequestItem;
import q.qedet.procurement.dto.ProcurementDtos.SupplierRequest;
import q.qedet.procurement.dto.ProcurementResponses.CurrencyTotal;
import q.qedet.procurement.dto.ProcurementResponses.ProductResponse;
import q.qedet.procurement.dto.ProcurementResponses.PurchaseOrderResponse;
import q.qedet.procurement.dto.ProcurementResponses.PurchaseRequestResponse;
import q.qedet.procurement.dto.ProcurementResponses.PurchaseRequestItemResponse;
import q.qedet.procurement.dto.ProcurementResponses.PurchaseOrderItemResponse;
import q.qedet.procurement.dto.ProcurementResponses.ProductSummary;
import q.qedet.procurement.dto.ProcurementResponses.SupplierResponse;
import q.qedet.product.entity.OrganizationProduct;
import q.qedet.product.entity.Product;
import q.qedet.product.entity.ProductStatus;
import q.qedet.product.entity.ProductType;
import q.qedet.procurement.entity.PurchaseOrder;
import q.qedet.procurement.entity.PurchaseOrderItem;
import q.qedet.procurement.entity.PurchaseRequest;
import q.qedet.procurement.entity.PurchaseRequestItem;
import q.qedet.procurement.entity.Supplier;
import q.qedet.product.repository.OrganizationProductRepository;
import q.qedet.product.repository.ProductRepository;
import q.qedet.procurement.repository.PurchaseOrderRepository;
import q.qedet.procurement.repository.PurchaseOrderItemRepository;
import q.qedet.procurement.repository.PurchaseRequestRepository;
import q.qedet.procurement.repository.ReceivingRecordRepository;
import q.qedet.procurement.repository.SupplierRepository;
import q.qedet.warehouse.service.ReceivingService;

@Service
public class ProcurementService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal DEFAULT_TAX_PERCENT = new BigDecimal("15");

    private final ActorContext actor;
    private final OrganizationRepository organizations;
    private final EmployeeRepository employees;
    private final ProductRepository products;
    private final OrganizationProductRepository organizationProducts;
    private final SupplierRepository suppliers;
    private final PurchaseRequestRepository requests;
    private final PurchaseOrderRepository orders;
    private final PurchaseOrderItemRepository orderItems;
    private final ReceivingRecordRepository receiving;
    private final ReceivingService receivingService;

    public ProcurementService(
            ActorContext actor,
            OrganizationRepository organizations,   
            EmployeeRepository employees,
            ProductRepository products,
            OrganizationProductRepository organizationProducts,
            SupplierRepository suppliers,
            PurchaseRequestRepository requests,
            PurchaseOrderRepository orders,
            PurchaseOrderItemRepository orderItems,
            ReceivingRecordRepository receiving,
            ReceivingService receivingService) {
        this.actor = actor;
        this.organizations = organizations;
        this.employees = employees;
        this.products = products;
        this.organizationProducts = organizationProducts;
        this.suppliers = suppliers;
        this.requests = requests;
        this.orders = orders;
        this.orderItems = orderItems;
        this.receiving = receiving;
        this.receivingService = receivingService;
    }

    public void requireOrganization(UUID organizationId) {
        if (!actor.organizationId().equals(organizationId)) {
            throw new NotFoundException("Organization not found");
        }
    }

    public List<ProductResponse> productList(int page, int limit, String search) {
        requirePermission(Permission.PRODUCT_VIEW);
        String normalizedSearch = normalizeSearch(search);

        return paginate(organizationProducts.findAllByOrganizationId(organizationId()).stream()
                .filter(organizationProduct -> containsIgnoreCase(organizationProduct.getOrganizationSku(), normalizedSearch)
                        || containsIgnoreCase(organizationProduct.getProduct().getName(), normalizedSearch))
                .map(this::productResponse)
                .toList(), page, limit);
    }

    public ProductResponse productGet(UUID id) {
        requirePermission(Permission.PRODUCT_VIEW);
        return productResponse(organizationProduct(id));
    }

    @Transactional
    public ProductResponse productCreate(ProductRequest request) {
        requirePermission(Permission.PRODUCT_MANAGE);
        if (organizationProducts.findAllByOrganizationId(organizationId()).stream()
                .anyMatch(product -> request.sku().trim().equalsIgnoreCase(product.getOrganizationSku()))) {
            throw new ConflictException("SKU already exists");
        }

        Product product = new Product();
        product.setName(request.name().trim());
        product.setProductType(globalProductType(request.type()));
        product.setStatus(ProductStatus.ACTIVE);
        Product saved = products.save(product);
        OrganizationProduct organizationProduct = new OrganizationProduct();
        organizationProduct.setOrganization(organization());
        organizationProduct.setProduct(saved);
        organizationProduct.setOrganizationSku(request.sku().trim());
        return productResponse(organizationProducts.save(organizationProduct));
    }

    public Product productEntity(String code) {
        requirePermission(Permission.PRODUCT_VIEW);
        return organizationProducts.findAllByOrganizationId(organizationId()).stream()
                .filter(product -> code.equalsIgnoreCase(product.getOrganizationSku()))
                .map(OrganizationProduct::getProduct)
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Product not found"));
    }

    @Transactional
    public SupplierResponse supplierCreate(SupplierRequest request) {
        requirePermission(Permission.SUPPLIER_MANAGE);
        if (supplierNameExists(request.name())) {
            throw new ConflictException("Supplier name already exists");
        }

        Supplier supplier = new Supplier();
        supplier.setOrganization(organization());
        applySupplierRequest(supplier, request, true);
        return supplierResponse(suppliers.save(supplier));
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> supplierList(int page, int limit, String search, String category) {
        requirePermission(Permission.SUPPLIER_VIEW);
        return paginate(filteredSuppliers(search, category).stream()
                .map(this::supplierResponse)
                .toList(), page, limit);
    }

    public long supplierTotal(String search, String category) {
        return filteredSuppliers(search, category).size();
    }


    @Transactional(readOnly = true)
    public SupplierResponse supplierGet(UUID id) {
        requirePermission(Permission.SUPPLIER_VIEW);
        return supplierResponse(findSupplier(id));
    }

    @Transactional
    public SupplierResponse supplierUpdate(UUID id, SupplierRequest request) {
        requirePermission(Permission.SUPPLIER_MANAGE);
        Supplier supplier = findSupplier(id);
        applySupplierRequest(supplier, request, false);
        return supplierResponse(suppliers.save(supplier));
    }

    @Transactional
    public SupplierResponse supplierStatus(UUID id, boolean active) {
        requirePermission(Permission.SUPPLIER_MANAGE);
        Supplier supplier = findSupplier(id);
        supplier.setStatus(active ? Supplier.SupplierStatus.Active : Supplier.SupplierStatus.Inactive);
        return supplierResponse(suppliers.save(supplier));
    }

    @Transactional
    public PurchaseRequestResponse requestCreate(PurchaseRequestRequest request) {
        requirePermission(Permission.PURCHASE_REQUEST_CREATE);
        validateRequestCreate(request);
        PurchaseRequest purchaseRequest = new PurchaseRequest();
        // Serializing number allocation per organization keeps generated PR numbers unique.
        purchaseRequest.setOrganization(lockedOrganization());
        purchaseRequest.setNumber(nextNumber("PR", requests.findAllByOrganizationId(organizationId()).size()));
        purchaseRequest.setRequestType(requestType(request.requestType()));
        purchaseRequest.setTitle(requiredText(request.title(), "title"));
        purchaseRequest.setReason(requiredText(request.reason(), "reason"));
        purchaseRequest.setRequestedBy(requesterId());
        purchaseRequest.setDepartment(request.department());
        purchaseRequest.setSourceDomain(request.sourceDomain());
        purchaseRequest.setSourceReference(request.sourceReference());
        purchaseRequest.setDate(LocalDate.now());
        purchaseRequest.setNeededBy(request.neededBy());
        purchaseRequest.setNotes(request.notes());
        purchaseRequest.setAttachmentReference(request.attachmentReference());
        purchaseRequest.setPriority(request.priority() == null ? PurchaseRequest.Priority.NORMAL
                : enumValue(PurchaseRequest.Priority.class, request.priority()));
        purchaseRequest.setProcurementStatus(PurchaseRequest.ProcurementStatus.SUBMITTED);
        purchaseRequest.setOriginatingRequestStatus(PurchaseRequest.OriginatingRequestStatus.OPEN);
        replaceRequestItems(purchaseRequest, request.items(), request.currency());
        return requestResponse(requests.save(purchaseRequest));
    }

    @Transactional
    public PurchaseRequestResponse requestUpdate(UUID id, PurchaseRequestUpdateRequest update) {
        requirePermission(Permission.PURCHASE_REQUEST_CREATE);
        PurchaseRequest request = findLockedRequest(id);
        requireOpenUnapprovedRequester(request);
        validateRequestUpdate(update);
        request.setTitle(requiredText(update.title(), "title"));
        request.setReason(requiredText(update.reason(), "reason"));
        request.setNeededBy(update.neededBy());
        request.setPriority(update.priority() == null ? PurchaseRequest.Priority.NORMAL
                : enumValue(PurchaseRequest.Priority.class, update.priority()));
        request.setNotes(update.notes());
        request.setAttachmentReference(update.attachmentReference());
        replaceRequestItems(request, update.items(), request.getCurrency());
        return requestResponse(requests.save(request));
    }
    @Transactional(readOnly = true)
    public List<PurchaseRequestResponse> requestList(
            int page, int limit, String search, String status, String priority, String department) {
        requirePermission(Permission.PURCHASE_REQUEST_VIEW);

        List<PurchaseRequest> filtered =
                filteredRequests(search, status, priority, department);

        System.out.println("Filtered PR count: " + filtered.size());

        List<PurchaseRequestResponse> mapped = filtered.stream()
                .map(this::requestResponse)
                .toList();

        System.out.println("Mapped PR count: " + mapped.size());

        List<PurchaseRequestResponse> result =
                paginate(mapped, page, limit);

        System.out.println("Paginated PR count: " + result.size());

        return result;

//        return paginate(filteredRequests(search, status, priority, department).stream()
//                .map(this::requestResponse)
//                .toList(), page, limit);
    }

    public long requestTotal(String search, String status, String priority, String department) {
        return filteredRequests(search, status, priority, department).size();
    }
    @Transactional(readOnly = true)
    public PurchaseRequestResponse requestGet(UUID id) {
        requirePermission(Permission.PURCHASE_REQUEST_VIEW);
        return requestResponse(findRequest(id));
    }

    @Transactional
    public PurchaseRequestResponse requestAction(UUID id, String action, String reason) {
        PurchaseRequest request = findLockedRequest(id);
        String normalizedAction = normalizeRequestAction(action);

        switch (normalizedAction) {
            case "SUBMIT" -> throw new ConflictException("Purchase requests are submitted on creation");
            case "APPROVE" -> approveRequest(request);
            case "REJECT" -> rejectRequest(request, reason);
            case "CANCEL" -> cancelRequest(request);
            default -> throw new BadRequestException("Invalid purchase request action");
        }
        return requestResponse(requests.save(request));
    }

    @Transactional
    public PurchaseOrderResponse orderCreate(PurchaseOrderRequest request) {
        requirePermission(Permission.PURCHASE_ORDER_CREATE);
        if (request.items() == null || request.items().isEmpty()) throw new BadRequestException("Purchase order requires at least one item");
        PurchaseRequest sourceRequest = findLockedRequest(request.purchaseRequestId());
        if (sourceRequest.getOriginatingRequestStatus() != PurchaseRequest.OriginatingRequestStatus.OPEN
                || sourceRequest.getProcurementStatus() != PurchaseRequest.ProcurementStatus.APPROVED)
            throw new ConflictException("Purchase request must be approved before ordering");
        Employee creator = currentEmployee();
        PurchaseOrder order = new PurchaseOrder();
        order.setOrganization(organization());
        order.setNumber(nextNumber("PO", orders.findAllByOrganizationId(organizationId()).size()));
        order.setPurchaseRequest(sourceRequest);
        order.setNotes(request.notes());

        order.setCreatedBy(creator);
        for (PurchaseOrderItemRequest item : request.items()) {
            addOrderItem(order, sourceRequest, creator, item);
        }
        return orderResponse(orders.save(order));
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> orderList(int page, int limit, String search, String status) {
        requirePermission(Permission.PURCHASE_ORDER_VIEW);
        return paginate(filteredOrders(search, status).stream()
                .map(this::orderResponse)
                .toList(), page, limit);
    }

    public long orderTotal(String search, String status) {
        return filteredOrders(search, status).size();
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse orderGet(UUID id) {
        requirePermission(Permission.PURCHASE_ORDER_VIEW);
        PurchaseOrder order = orders
                .findByIdWithItemsAndOrganizationId(id, organizationId())
                .orElseThrow(() ->
                        new NotFoundException("Purchase order not found"));

        return orderResponse(order);
    }

    @Transactional
    public PurchaseOrderResponse orderAction(UUID id, String action) {
        String normalizedAction = normalizeAction(action);
        requireOrderActionPermission(normalizedAction);

        PurchaseOrder order = findLockedOrder(id);
        PurchaseOrder.Status nextStatus = orderStatus(normalizedAction);
        if (!validOrderTransition(order.getStatus(), nextStatus)) {
            throw new ConflictException("Invalid purchase order state transition");
        }
        if (nextStatus == PurchaseOrder.Status.CLOSED && order.getItems().stream().anyMatch(item ->
                item.getOrderStatus() != PurchaseOrderItem.OrderStatus.CLOSED
                        && item.getOrderStatus() != PurchaseOrderItem.OrderStatus.CANCELLED))
            throw new ConflictException("All purchase order items must be closed or cancelled before closing the order");
        if (nextStatus == PurchaseOrder.Status.CANCELLED && order.getItems().stream().anyMatch(item ->
                item.getReceivingStatus() != PurchaseOrderItem.ReceivingStatus.NOT_STARTED))
            throw new ConflictException("Purchase order with receiving activity cannot be cancelled");
        order.setStatus(nextStatus);
        return orderResponse(orders.save(order));
    }

    @Transactional
    public PurchaseOrderResponse itemAction(UUID orderId, UUID itemId, String action) {
        requirePermission(Permission.PURCHASE_ORDER_UPDATE);
        PurchaseOrder order = findLockedOrder(orderId);
        PurchaseOrderItem item = order.getItems().stream().filter(value -> value.getId().equals(itemId))
                .findFirst().orElseThrow(() -> new NotFoundException("Purchase order item not found"));
        PurchaseOrderItem.OrderStatus next = enumValue(PurchaseOrderItem.OrderStatus.class, normalizeAction(action));
        boolean allowed = switch (item.getOrderStatus()) {
            case DRAFT -> next == PurchaseOrderItem.OrderStatus.CONFIRMED || next == PurchaseOrderItem.OrderStatus.CANCELLED;
            case CONFIRMED -> next == PurchaseOrderItem.OrderStatus.SENT_TO_SUPPLIER || next == PurchaseOrderItem.OrderStatus.CANCELLED;
            case SENT_TO_SUPPLIER -> next == PurchaseOrderItem.OrderStatus.SUPPLIER_CONFIRMED || next == PurchaseOrderItem.OrderStatus.CANCELLED;
            case SUPPLIER_CONFIRMED -> next == PurchaseOrderItem.OrderStatus.READY_TO_RECEIVE || next == PurchaseOrderItem.OrderStatus.CANCELLED;
            case READY_TO_RECEIVE -> next == PurchaseOrderItem.OrderStatus.CLOSED;
            default -> false;
        };
        if (!allowed) throw new ConflictException("Invalid purchase order item state transition");
        if (next == PurchaseOrderItem.OrderStatus.CLOSED && item.getReceivingStatus() != PurchaseOrderItem.ReceivingStatus.FULLY_RECEIVED)
            throw new ConflictException("Purchase order item cannot close before all quantities are accounted for");
        if (next == PurchaseOrderItem.OrderStatus.CANCELLED && item.getReceivingStatus() != PurchaseOrderItem.ReceivingStatus.NOT_STARTED)
            throw new ConflictException("Purchase order item with receiving activity cannot be cancelled");
        item.setOrderStatus(next);
        if (next == PurchaseOrderItem.OrderStatus.READY_TO_RECEIVE) {
            receivingService.reconcileReadyItem(order, item);
        }
        return orderResponse(orders.save(order));
    }

    private Organization organization() {
        return organizations.findById(organizationId())
                .orElseThrow(() -> new NotFoundException("Organization not found"));
    }

    private Organization lockedOrganization() {
        return organizations.findByIdForUpdate(organizationId())
                .orElseThrow(() -> new NotFoundException("Organization not found"));
    }

    private UUID organizationId() {
        return actor.organizationId();
    }

    private void requirePermission(Permission permission) {
        actor.requirePermission(permission);
    }

    private Product findProduct(UUID id) {
        if (!organizationProducts.existsByOrganizationIdAndProductId(organizationId(), id)) {
            throw new NotFoundException("Product not found");
        }
        return products.findById(id).orElseThrow(() -> new NotFoundException("Product not found"));
    }

    private Product findProduct(String id) {
        return findProduct(parseId(id, "productId"));
    }

    private Supplier findActiveSupplier(String id) {
        Supplier supplier = findSupplier(parseId(id, "supplierId"));
        if (supplier.getStatus() != Supplier.SupplierStatus.Active) {
            throw new ConflictException("Supplier is not active");
        }
        return supplier;
    }

    private Supplier findSupplier(UUID id) {
        return suppliers.findByIdAndOrganizationId(id, organizationId())
                .orElseThrow(() -> new NotFoundException("Supplier not found"));
    }

    private PurchaseRequest findRequest(UUID id) {
        return requests.findByIdAndOrganizationId(id, organizationId())
                .orElseThrow(() -> new NotFoundException("Purchase request not found"));
    }

    private PurchaseRequest findLockedRequest(UUID id) {
        return requests.findLockedByIdAndOrganizationId(id, organizationId())
                .orElseThrow(() -> new NotFoundException("Purchase request not found"));
    }

    private PurchaseOrder findOrder(UUID id) {
        return orders.findByIdAndOrganizationId(id, organizationId())
                .orElseThrow(() -> new NotFoundException("Purchase order not found"));
    }

    private PurchaseOrder findLockedOrder(UUID id) {
        return orders.findLockedByIdAndOrganizationId(id, organizationId())
                .orElseThrow(() -> new NotFoundException("Purchase order not found"));
    }

    private Employee currentEmployee() {
        return employees.findByIdAndOrganizationId(actor.employeeId(), organizationId())
                .orElseThrow(() -> new NotFoundException("Employee not found"));
    }

    private boolean supplierNameExists(String name) {
        return suppliers.findAllByOrganizationId(organizationId()).stream()
                .anyMatch(supplier -> supplier.getName().equalsIgnoreCase(name.trim()));
    }

    private void applySupplierRequest(Supplier supplier, SupplierRequest request, boolean includeCurrency) {
        supplier.setName(request.name().trim());
        supplier.setContact(request.contact());
        supplier.setEmail(request.email());
        supplier.setPhone(request.phone());
        supplier.setCategory(request.category());
        supplier.setCity(request.city());
        supplier.setCountry(request.country());
        supplier.setAddress(request.address());
        supplier.setPaymentTerms(request.paymentTerms());
        supplier.setLeadTimeDays(request.leadTimeDays());
        if (includeCurrency && request.currency() != null) {
            supplier.setCurrency(currency(request.currency()));
        }
    }

    private String currency(String value) {
        if (!value.matches("(?i)[a-z]{3}")) {
            throw new BadRequestException("Currency must be a three-letter code");
        }
        return value.toUpperCase(Locale.ROOT);
    }

    private List<Supplier> filteredSuppliers(String search, String category) {
        String normalizedSearch = normalizeSearch(search);
        return suppliers.findAllByOrganizationId(organizationId()).stream()
                .filter(supplier -> containsIgnoreCase(supplier.getName(), normalizedSearch))
                .filter(supplier -> category == null || category.equalsIgnoreCase(supplier.getCategory()))
                .toList();
    }

    private List<PurchaseRequest> filteredRequests(
            String search, String status, String priority, String department) {
        String normalizedSearch = normalizeSearch(search);
        return requests.findAllByOrganizationId(organizationId()).stream()
                .filter(request -> containsIgnoreCase(request.getNumber(), normalizedSearch))
                .filter(request -> status == null
                        || request.getProcurementStatus().name().equalsIgnoreCase(status)
                        || request.getOriginatingRequestStatus().name().equalsIgnoreCase(status))
                .filter(request -> priority == null || request.getPriority().name().equalsIgnoreCase(priority))
                .filter(request -> department == null || department.equalsIgnoreCase(request.getDepartment()))
                .toList();
    }

    private List<PurchaseOrder> filteredOrders(String search, String status) {
        String normalizedSearch = normalizeSearch(search);
        String normalizedStatus =
                status == null || status.isBlank() ? null : status.trim();
        return orders.findAllByOrganizationId(organizationId()).stream()
                .filter(order -> containsIgnoreCase(
                        order.getNumber(), normalizedSearch))
                .filter(order -> normalizedStatus == null
                        || order.getStatus().name()
                        .equalsIgnoreCase(normalizedStatus))
                .toList();
    }

    private void replaceRequestItems(PurchaseRequest request, List<RequestItem> items, String requestedCurrency) {
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("Purchase request requires at least one item");
        }
        String headerCurrency = requestedCurrency == null ? request.getCurrency() : currency(requestedCurrency);
        String resolvedCurrency = headerCurrency;
        for (RequestItem input : items) {
            if (input.unitPrice() == null) {
                continue;
            }
            String itemCurrency = input.currency() == null ? headerCurrency : currency(input.currency());
            if (itemCurrency == null) {
                throw new BadRequestException("currency is required when an item has a unitPrice");
            }
            if (resolvedCurrency == null) {
                resolvedCurrency = itemCurrency;
            } else if (!resolvedCurrency.equals(itemCurrency)) {
                throw new BadRequestException("All priced request items must use the request currency");
            }
        }

        request.getItems().clear();
        BigDecimal total = BigDecimal.ZERO;
        for (RequestItem input : items) {
            BigDecimal quantity = input.qty() == null ? input.quantity() : input.qty();
            if (quantity == null || quantity.signum() <= 0) {
                throw new BadRequestException("qty must be positive");
            }
            if (input.unitPrice() != null && input.unitPrice().signum() < 0) {
                throw new BadRequestException("unitPrice must not be negative");
            }
            PurchaseRequestItem item = new PurchaseRequestItem();
            item.setPurchaseRequest(request);
            if (input.productId() != null && !input.productId().isBlank()) {
                Product product = findProduct(input.productId());
                item.setProduct(product);
                item.setProductName(product.getName());
                item.setSku(organizationProduct(product.getId()).getOrganizationSku());
                item.setUnit(input.unit() == null || input.unit().isBlank()
                        ? (product.getUnit() == null ? null : product.getUnit().getSymbol()) : input.unit().trim());
            } else {
                String productName = input.productName() == null || input.productName().isBlank()
                        ? input.name() : input.productName();
                if (productName == null || productName.isBlank()) {
                    throw new BadRequestException("productName is required for non-catalog items");
                }
                item.setProductName(productName.trim());
                item.setUnit(input.unit() == null ? null : input.unit().trim());
            }
            item.setItemCategory(firstNonBlank(input.itemCategory(), input.productType()));
            item.setDescription(input.description());
            item.setQty(quantity);
            item.setUnitPrice(input.unitPrice() == null ? null : money(input.unitPrice()));
            item.setCurrency(input.unitPrice() == null ? null : resolvedCurrency);
            item.setNotes(input.notes());
            request.getItems().add(item);
            if (item.getUnitPrice() != null) {
                total = total.add(item.getQty().multiply(item.getUnitPrice()));
            }
        }
        request.setCurrency(resolvedCurrency);
        request.setTotalEstimate(money(total));
    }

    private String normalizeRequestAction(String action) {
        String normalized = normalizeAction(action);
        return switch (normalized) {
            case "START_REVIEW", "RESUBMIT_INFORMATION" -> "SUBMIT";
            case "REQUEST_INFORMATION" -> throw new BadRequestException(
                    "REQUEST_INFORMATION requires a review status not supported by the current purchase request model");
            case "COMPLETE" -> "COMPLETE";
            default -> normalized;
        };
    }

    private void approveRequest(PurchaseRequest request) {
        requirePermission(Permission.PURCHASE_ORDER_APPROVE);
        if (request.getOriginatingRequestStatus() != PurchaseRequest.OriginatingRequestStatus.OPEN
                || request.getProcurementStatus() != PurchaseRequest.ProcurementStatus.SUBMITTED) {
            throw new ConflictException("Only open submitted requests can be approved");
        }
        request.setProcurementStatus(PurchaseRequest.ProcurementStatus.APPROVED);
    }

    private void rejectRequest(PurchaseRequest request, String reason) {
        requirePermission(Permission.PURCHASE_ORDER_APPROVE);
        if (request.getOriginatingRequestStatus() != PurchaseRequest.OriginatingRequestStatus.OPEN
                || request.getProcurementStatus() != PurchaseRequest.ProcurementStatus.SUBMITTED) {
            throw new ConflictException("Only open submitted requests can be rejected");
        }
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("Rejection reason is required");
        }
        request.setProcurementStatus(PurchaseRequest.ProcurementStatus.REJECTED);
    }

    private void cancelRequest(PurchaseRequest request) {
        requirePermission(Permission.PURCHASE_REQUEST_CREATE);
        requireOpenUnapprovedRequester(request);
        request.setOriginatingRequestStatus(PurchaseRequest.OriginatingRequestStatus.CANCELLED);
    }

    private void requireOpenUnapprovedRequester(PurchaseRequest request) {
        if (!requesterId().equals(request.getRequestedBy())) {
            throw new ForbiddenException("Only the originating requester may change this purchase request");
        }
        if (request.getOriginatingRequestStatus() != PurchaseRequest.OriginatingRequestStatus.OPEN) {
            throw new ConflictException("Cancelled purchase requests cannot be changed");
        }
        if (request.getProcurementStatus() == PurchaseRequest.ProcurementStatus.APPROVED) {
            throw new ConflictException("Approved purchase requests require an amendment");
        }
    }


    private void addOrderItem(
            PurchaseOrder order,
            PurchaseRequest request,
            Employee creator,
            PurchaseOrderItemRequest input
    ) {
        PurchaseRequestItem source = request.getItems().stream()
                .filter(item -> item.getId().equals(input.purchaseRequestItemId()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Purchase request item does not belong to the source request"
                ));

        BigDecimal allocated = orderItems
                .findAllByPurchaseRequestItemId(source.getId()).stream()
                .filter(item -> item.getOrderStatus() != PurchaseOrderItem.OrderStatus.CANCELLED)
                .map(PurchaseOrderItem::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal inThisOrder = order.getItems().stream()
                .filter(item -> source.equals(item.getPurchaseRequestItem()))
                .map(PurchaseOrderItem::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (allocated.add(inThisOrder).add(input.quantity())
                .compareTo(source.getQty()) > 0) {
            throw new ConflictException(
                    "Purchase order quantity exceeds the approved request item remaining quantity"
            );
        }

        Supplier supplier = findSupplier(input.supplierId());

        if (supplier.getStatus() != Supplier.SupplierStatus.Active) {
            throw new ConflictException("Supplier is not active");
        }

        PurchaseOrderItem item = new PurchaseOrderItem();

        item.setPurchaseOrder(order);
        item.setPurchaseRequestItem(source);
        item.setSupplier(supplier);

        // Persist the selected product type.
        String requestedType = input.productType();

        PurchaseOrderItem.ProductType productType;


        if (requestedType == null || requestedType.isBlank()) {
            throw new BadRequestException("Product type is required");
        }



        try {
            productType = PurchaseOrderItem.ProductType.valueOf(
                    requestedType.trim().toUpperCase(java.util.Locale.ROOT)
            );
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid product type: " + requestedType);
        }

        item.setProductType(productType);

        item.setOrderDate(
                input.orderDate() == null ? LocalDate.now() : input.orderDate()
        );
        item.setExpectedDeliveryDate(input.expectedDeliveryDate());
        item.setCurrency(currency(input.currency()));
        item.setPaymentTerms(input.paymentTerms());
        item.setPaymentMethod(
                enumValue(PurchaseOrderItem.PaymentMethod.class, input.paymentMethod())
        );
        item.setPaymentTiming(
                enumValue(PurchaseOrderItem.PaymentTiming.class, input.paymentTiming())
        );
        item.setQuantity(input.quantity());

        item.setUnit(requiredText(input.unit(), "unit"));

        item.setTotalPrice(money(input.totalPrice()));
        item.setDiscountPercent(
                input.discountPercent() == null
                        ? BigDecimal.ZERO
                        : input.discountPercent()
        );
        item.setDiscountAmount(money(input.discountAmount()));
        item.setTaxAmount(money(input.taxAmount()));
        item.setShippingAmount(money(input.shippingAmount()));
        item.setCreatedBy(creator);

        order.getItems().add(item);
    }


    private void requireOrderActionPermission(String action) {
        if ("CONFIRM".equals(action) || "RECORD_SUPPLIER_CONFIRMATION".equals(action)) {
            requirePermission(Permission.PURCHASE_ORDER_APPROVE);
            return;
        }
        requirePermission(Permission.PURCHASE_ORDER_UPDATE);
    }

    private PurchaseOrder.Status orderStatus(String action) {
        return switch (action) {
            case "CONFIRM" -> PurchaseOrder.Status.CONFIRMED;
            case "CANCEL" -> PurchaseOrder.Status.CANCELLED;
            case "CLOSE" -> PurchaseOrder.Status.CLOSED;
            default -> throw new BadRequestException("Invalid purchase order action");
        };
    }

    private boolean validOrderTransition(PurchaseOrder.Status current, PurchaseOrder.Status next) {
        return switch (current) {
            case DRAFT -> next == PurchaseOrder.Status.CONFIRMED || next == PurchaseOrder.Status.CANCELLED;
            case CONFIRMED -> next == PurchaseOrder.Status.CLOSED || next == PurchaseOrder.Status.CANCELLED;
            default -> false;
        };
    }

    private ProductResponse productResponse(OrganizationProduct organizationProduct) {
        Product product = organizationProduct.getProduct();
        return new ProductResponse(
                product.getId(), organizationProduct.getOrganizationSku(), product.getName(),
                product.getBrand() == null ? null : product.getBrand().getName(),
                product.getCategory() == null ? null : product.getCategory().getName(),
                product.getProductType().name(), product.getUnit() == null ? null : product.getUnit().getSymbol(),
                null, null, null, null, null, null,
                product.getStatus().name());
    }

    private SupplierResponse supplierResponse(Supplier supplier) {
        List<PurchaseOrderItem> supplierItems = orders.findAllByOrganizationId(organizationId()).stream()
                .flatMap(order -> order.getItems().stream())
                .filter(item -> item.getSupplier().getId().equals(supplier.getId()))
                .toList();
        BigDecimal spend = supplierItems.stream()
                .filter(item -> item.getOrderStatus() != PurchaseOrderItem.OrderStatus.CANCELLED)
                .map(PurchaseOrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SupplierResponse(
                supplier.getId(), supplier.getName(), supplier.getContact(), supplier.getEmail(),
                supplier.getPhone(), supplier.getCategory(), supplier.getCity(), supplier.getCountry(),
                supplier.getAddress(), supplier.getPaymentTerms(), supplier.getLeadTimeDays(), supplier.getCurrency(),
                null, null, null, supplierItems.size(), spend, supplier.getStatus().name());
    }

    private PurchaseRequestResponse requestResponse(PurchaseRequest request) {
        return new PurchaseRequestResponse(
                request.getId(), request.getNumber(), request.getRequestedBy(), request.getRequestType().name(),
                request.getTitle(), request.getReason(), request.getDepartment(), request.getSourceDomain(),
                request.getSourceReference(), request.getDate(), request.getNeededBy(), request.getPriority().name(),
                request.getOriginatingRequestStatus() == PurchaseRequest.OriginatingRequestStatus.CANCELLED
                        ? PurchaseRequest.OriginatingRequestStatus.CANCELLED.name()
                        : request.getProcurementStatus().name(),
                request.getProcurementStatus().name(), request.getNotes(), request.getAttachmentReference(),
                request.getItems().stream()
                        .map(item -> new PurchaseRequestItemResponse(
                                item.getId(),
                                item.getProduct() == null ? null : item.getProduct().getId().toString(),
                                item.getProductName(), item.getProductName(), item.getSku(), item.getItemCategory(),
                                item.getItemCategory(), item.getDescription(), item.getQty(), item.getQty(), item.getUnit(),
                                item.getUnitPrice(), item.getCurrency(), item.getNotes()))
                        .toList(),
                request.getTotalEstimate(), request.getCurrency(), request.getCreatedAt(), request.getUpdatedAt());
    }


    private PurchaseOrderResponse orderResponse(PurchaseOrder order) {
        return new PurchaseOrderResponse(
                order.getId(),
                order.getNumber(),
                order.getPurchaseRequest().getId(),
                order.getPurchaseRequest().getNumber(),
                order.getStatus().name(),
                order.getNotes(),
                order.getCreatedBy().getId(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getItems().stream()
                        .map(item -> new PurchaseOrderItemResponse(
                                item.getId(),
                                item.getPurchaseRequestItem().getId(),
                                item.getProductType() == null
                                        ? "STANDARD_UNIT"
                                        : item.getProductType().name(),
                                productSummary(item.getPurchaseRequestItem()),
                                item.getSupplier().getId(),
                                item.getSupplier().getName(),
                                item.getOrderDate(),
                                item.getExpectedDeliveryDate(),
                                item.getCurrency(),
                                item.getPaymentTerms(),
                                item.getPaymentMethod() == null
                                        ? null : item.getPaymentMethod().name(),
                                item.getPaymentTiming() == null
                                        ? null : item.getPaymentTiming().name(),
                                item.getQuantity(),
                                item.getUnit(),
                                item.getTotalPrice(),
                                item.getDiscountPercent(),
                                item.getDiscountAmount(),
                                item.getTaxAmount(),
                                item.getShippingAmount(),
                                item.getOrderStatus().name(),
                                item.getReceivingStatus().name(),
                                item.getCreatedBy().getId()
                        ))
                        .toList(),
                order.getItems().stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                PurchaseOrderItem::getCurrency,
                                java.util.LinkedHashMap::new,
                                java.util.stream.Collectors.reducing(
                                        BigDecimal.ZERO,
                                        PurchaseOrderItem::getTotalPrice,
                                        BigDecimal::add
                                )
                        ))
                        .entrySet().stream()
                        .map(entry -> new CurrencyTotal(
                                entry.getKey(), entry.getValue()
                        ))
                        .toList()
        );
    }


    private ProductSummary productSummary(PurchaseRequestItem requestItem) {
        Product product = requestItem.getProduct();
        if (product == null) {
            return new ProductSummary(null, requestItem.getSku(), requestItem.getProductName(), requestItem.getUnit(),
                    requestItem.getDescription(), false);
        }
        return new ProductSummary(product.getId(), organizationProduct(product.getId()).getOrganizationSku(), product.getName(),
                product.getUnit() == null ? null : product.getUnit().getSymbol(),
                requestItem.getDescription(), true);
    }

    private OrganizationProduct organizationProduct(UUID productId) {
        return organizationProducts.findByOrganizationIdAndProductId(organizationId(), productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
    }

    private ProductType globalProductType(String type) {
        if (type == null || type.isBlank()) {
            return ProductType.STANDARD;
        }
        try {
            return ProductType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Invalid product type");
        }
    }

    private <T> List<T> paginate(List<T> all, int page, int limit) {
        System.out.println("Input page: " + page);
        System.out.println("Input limit: " + limit);

        int pageSize = Math.min(Math.max(limit, 1), 100);
        int pageNumber = Math.max(page, 1);
        int from = Math.min((pageNumber - 1) * pageSize, all.size());
        System.out.println("Calculated from: " + from);

        return all.subList(from, Math.min(from + pageSize, all.size()));
    }

    private String normalizeSearch(String search) {
        return search == null ? "" : search.toLowerCase(Locale.ROOT);
    }

    private boolean containsIgnoreCase(String value, String search) {
        return value.toLowerCase(Locale.ROOT).contains(search);
    }

    private String normalizeAction(String action) {
        return action == null ? "" : action.trim().toUpperCase(Locale.ROOT);
    }

    private void validateRequestCreate(PurchaseRequestRequest request) {
        if (request == null) {
            throw new BadRequestException("Purchase request is required");
        }
        requiredText(request.title(), "title");
        requiredText(request.reason(), "reason");
        if (request.items() == null || request.items().isEmpty()) {
            throw new BadRequestException("Purchase request requires at least one item");
        }
        requestType(request.requestType());
        if (request.priority() != null) {
            enumValue(PurchaseRequest.Priority.class, request.priority());
        }
    }

    private void validateRequestUpdate(PurchaseRequestUpdateRequest request) {
        if (request == null) {
            throw new BadRequestException("Purchase request update is required");
        }
        requiredText(request.title(), "title");
        requiredText(request.reason(), "reason");
        if (request.items() == null || request.items().isEmpty()) {
            throw new BadRequestException("Purchase request requires at least one item");
        }
        if (request.priority() != null) {
            enumValue(PurchaseRequest.Priority.class, request.priority());
        }
    }

    private PurchaseRequest.RequestType requestType(String value) {
        return enumValue(PurchaseRequest.RequestType.class, value);
    }

    private UUID requesterId() {
        UUID identity = actor.employeeId() == null ? actor.userId() : actor.employeeId();
        if (identity == null) {
            throw new BadRequestException("Authenticated requester identity is required");
        }
        return identity;
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        return fallback == null || fallback.isBlank() ? null : fallback.trim();
    }

    private String requiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(field + " is required");
        }
        return value.trim();
    }

    private UUID parseId(String value, String field) {
        if (value == null) {
            throw new BadRequestException("Invalid " + field);
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Invalid " + field);
        }
    }

    private void validatePercentage(BigDecimal value, String field) {
        if (value != null && (value.signum() < 0 || value.compareTo(ONE_HUNDRED) > 0)) {
            throw new BadRequestException(field + " must be between 0 and 100");
        }
    }

    private String nextNumber(String prefix, long currentCount) {
        return prefix + "-" + LocalDate.now().getYear() + "-" + String.format("%04d", currentCount + 1);
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private <E extends Enum<E>> E enumValue(Class<E> type, String value) {
        if (value == null) {
            throw new BadRequestException("Invalid status value: null");
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT).replace(' ', '_'));
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Invalid status value: " + value);
        }
    }
}
