package q.qedet.procurement.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import q.qedet.common.exception.BadRequestException;
import q.qedet.procurement.dto.ProcurementDtos.*;
import q.qedet.procurement.service.ProcurementService;

import java.util.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/procurement")
public class OrganizationProcurementController {
    private final ProcurementService service;

    public OrganizationProcurementController(ProcurementService service) {
        this.service = service;
    }

    @GetMapping("/suppliers")
    public Map<String, Object> suppliers(@PathVariable UUID organizationId, @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String search,
                                         @RequestParam(required = false) String category) {
        service.requireOrganization(organizationId);
        List<?> items = service.supplierList(page + 1, size, search, category);
        return envelope(items, service.supplierTotal(search, category), page, size);
    }

    @GetMapping("/suppliers/{supplierId}")
    public Object supplier(@PathVariable UUID organizationId, @PathVariable UUID supplierId) {
        service.requireOrganization(organizationId);
        return service.supplierGet(supplierId);
    }

    @PostMapping("/suppliers")
    public Object createSupplier(@PathVariable UUID organizationId, @Valid @RequestBody SupplierRequest request) {
        service.requireOrganization(organizationId);
        return service.supplierCreate(request);
    }

    @PatchMapping("/suppliers/{supplierId}")
    public Object updateSupplier(@PathVariable UUID organizationId, @PathVariable UUID supplierId, @Valid @RequestBody SupplierRequest request) {
        service.requireOrganization(organizationId);
        return service.supplierUpdate(supplierId, request);
    }

    @GetMapping("/purchase-requests")
    public Map<String, Object> requests(@PathVariable UUID organizationId, @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String search,
                                        @RequestParam(required = false) String status, @RequestParam(required = false) String priority,
                                        @RequestParam(required = false) String department) {
        service.requireOrganization(organizationId);
        List<?> items = service.requestList(page + 1, size, search, status, priority, department);
        return envelope(items, service.requestTotal(search, status, priority, department), page, size);
    }

    @GetMapping("/purchase-requests/{requestId}")
    public Object request(@PathVariable UUID organizationId, @PathVariable UUID requestId) {
        service.requireOrganization(organizationId);
        return service.requestGet(requestId);
    }

    @PostMapping("/purchase-requests")
    public Object createRequest(@PathVariable UUID organizationId, @Valid @RequestBody PurchaseRequestRequest request) {
        service.requireOrganization(organizationId);
        return service.requestCreate(request);
    }

    @PatchMapping("/purchase-requests/{requestId}")
    public Object updateRequest(@PathVariable UUID organizationId, @PathVariable UUID requestId,
                                @Valid @RequestBody PurchaseRequestUpdateRequest request) {
        service.requireOrganization(organizationId);
        return service.requestUpdate(requestId, request);
    }

    @PostMapping("/purchase-requests/{requestId}/actions")
    public Object requestAction(@PathVariable UUID organizationId, @PathVariable UUID requestId, @RequestBody Map<String, String> body) {
        service.requireOrganization(organizationId);
        String action = body.get("action");
        if (action == null || action.isBlank()) throw new BadRequestException("action is required");
        return service.requestAction(requestId, action, body.get("comment"));
    }

    @GetMapping("/purchase-orders")
    public Map<String, Object> orders(@PathVariable UUID organizationId, @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String search,
                                      @RequestParam(required = false) String status) {
        service.requireOrganization(organizationId);
        List<?> items = service.orderList(page + 1, size, search, status);
        return envelope(items, service.orderTotal(search, status), page, size);
    }

    @GetMapping("/purchase-orders/{purchaseOrderId}")
    public Object order(@PathVariable UUID organizationId, @PathVariable UUID purchaseOrderId) {
        service.requireOrganization(organizationId);
        return service.orderGet(purchaseOrderId);
    }

    @PostMapping("/purchase-orders")
    public Object createOrder(@PathVariable UUID organizationId, @Valid @RequestBody PurchaseOrderRequest request) {
        service.requireOrganization(organizationId);
        return service.orderCreate(request);
    }

    @PostMapping("/purchase-orders/{purchaseOrderId}/actions")
    public Object orderAction(@PathVariable UUID organizationId, @PathVariable UUID purchaseOrderId, @RequestBody Map<String, String> body) {
        service.requireOrganization(organizationId);
        String action = body.get("action");
        if (action == null || action.isBlank()) throw new BadRequestException("action is required");
        return service.orderAction(purchaseOrderId, action);
    }

    @PostMapping("/purchase-orders/{purchaseOrderId}/items/{itemId}/actions")
    public Object itemAction(@PathVariable UUID organizationId, @PathVariable UUID purchaseOrderId, @PathVariable UUID itemId,
                             @RequestBody Map<String, String> body) {
        service.requireOrganization(organizationId);
        String action = body.get("action");
        if (action == null || action.isBlank()) throw new BadRequestException("action is required");
        return service.itemAction(purchaseOrderId, itemId, action);
    }

    private Map<String, Object> envelope(List<?> items, long total, int page, int size) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items); result.put("total", total); result.put("page", Math.max(page, 0)); result.put("size", Math.max(size, 1));
        return result;
    }
}
