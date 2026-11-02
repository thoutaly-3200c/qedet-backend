package q.qedet.procurement.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import q.qedet.procurement.dto.ProcurementDtos.*;
import q.qedet.procurement.dto.ProcurementResponses.*;
import q.qedet.procurement.service.ProcurementService;
import java.util.*;

@RestController
@RequestMapping("/api/v1/procurement")
public class ProcurementController {
    private final ProcurementService service;
    public ProcurementController(ProcurementService service) { this.service = service; }

    @GetMapping("/products")
    public List<ProductResponse> products(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int limit,@RequestParam(required=false) String search) { return service.productList(page,limit,search); }
    @GetMapping("/products/{id}") public ProductResponse product(@PathVariable UUID id) { return service.productGet(id); }
    @GetMapping("/products/by-barcode/{barcode}") public ProductResponse barcode(@PathVariable String barcode) { return service.productGet(service.productEntity(barcode).getId()); }
    @GetMapping("/products/by-sku/{sku}") public ProductResponse sku(@PathVariable String sku) { return service.productGet(service.productEntity(sku).getId()); }
    @PostMapping("/products") public ProductResponse createProduct(@Valid @RequestBody ProductRequest r) { return service.productCreate(r); }

    @GetMapping("/suppliers") public List<SupplierResponse> suppliers(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int limit,@RequestParam(required=false) String search,@RequestParam(required=false) String category) { return service.supplierList(page,limit,search,category); }
    @GetMapping("/suppliers/{id}") public SupplierResponse supplier(@PathVariable UUID id) { return service.supplierGet(id); }
    @PostMapping("/suppliers") public SupplierResponse createSupplier(@Valid @RequestBody SupplierRequest r) { return service.supplierCreate(r); }
    @PutMapping("/suppliers/{id}") public SupplierResponse updateSupplier(@PathVariable UUID id,@Valid @RequestBody SupplierRequest r) { return service.supplierUpdate(id,r); }
    @PostMapping("/suppliers/{id}/activate") public SupplierResponse activateSupplier(@PathVariable UUID id) { return service.supplierStatus(id,true); }
    @PostMapping("/suppliers/{id}/deactivate") public SupplierResponse deactivateSupplier(@PathVariable UUID id) { return service.supplierStatus(id,false); }

    @GetMapping("/purchase-requests") public List<PurchaseRequestResponse> requests(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int limit,@RequestParam(required=false) String search,@RequestParam(required=false) String status,@RequestParam(required=false) String priority,@RequestParam(required=false) String department) { return service.requestList(page,limit,search,status,priority,department); }
    @GetMapping("/purchase-requests/{id}") public PurchaseRequestResponse request(@PathVariable UUID id) { return service.requestGet(id); }
    @PostMapping("/purchase-requests") public PurchaseRequestResponse createRequest(@Valid @RequestBody PurchaseRequestRequest r) { return service.requestCreate(r); }
    @PatchMapping("/purchase-requests/{id}") public PurchaseRequestResponse updateRequest(@PathVariable UUID id,@Valid @RequestBody PurchaseRequestUpdateRequest r) { return service.requestUpdate(id,r); }
    @PostMapping("/purchase-requests/{id}/cancel") public PurchaseRequestResponse cancel(@PathVariable UUID id) { return service.requestAction(id,"cancel",null); }
    @PostMapping("/purchase-requests/{id}/submit") public PurchaseRequestResponse submit(@PathVariable UUID id) { return service.requestAction(id,"submit",null); }
    @PostMapping("/purchase-requests/{id}/approve") public PurchaseRequestResponse approve(@PathVariable UUID id) { return service.requestAction(id,"approve",null); }
    @PostMapping("/purchase-requests/{id}/reject") public PurchaseRequestResponse reject(@PathVariable UUID id,@RequestBody(required=false) Map<String,String> body) { return service.requestAction(id,"reject",body==null?null:body.get("reason")); }
    @PostMapping("/purchase-requests/{id}/complete") public PurchaseRequestResponse complete(@PathVariable UUID id) { return service.requestAction(id,"complete",null); }

    @GetMapping("/purchase-orders") public List<PurchaseOrderResponse> orders(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int limit,@RequestParam(required=false) String search,@RequestParam(required=false) String status) { return service.orderList(page,limit,search,status); }
    @GetMapping("/purchase-orders/{id}") public PurchaseOrderResponse order(@PathVariable UUID id) { return service.orderGet(id); }
    @PostMapping("/purchase-orders") public PurchaseOrderResponse createOrder(@Valid @RequestBody PurchaseOrderRequest r) { return service.orderCreate(r); }
    @PostMapping("/purchase-orders/{id}/approve") public PurchaseOrderResponse approveOrder(@PathVariable UUID id) { return service.orderAction(id,"approve"); }
    @PostMapping("/purchase-orders/{id}/send") public PurchaseOrderResponse sendOrder(@PathVariable UUID id) { return service.orderAction(id,"send"); }
    @PostMapping("/purchase-orders/{id}/confirm") public PurchaseOrderResponse confirmOrder(@PathVariable UUID id) { return service.orderAction(id,"confirm"); }
    @PostMapping("/purchase-orders/{id}/cancel") public PurchaseOrderResponse cancelOrder(@PathVariable UUID id) { return service.orderAction(id,"cancel"); }
    @PostMapping("/purchase-orders/{orderId}/items/{itemId}/actions") public PurchaseOrderResponse itemAction(@PathVariable UUID orderId,@PathVariable UUID itemId,@RequestBody Map<String,String> body) { return service.itemAction(orderId,itemId,body.get("action")); }
    @PostMapping("/purchase-orders/{id}/save-draft") public PurchaseOrderResponse saveDraft(@PathVariable UUID id) { return service.orderGet(id); }
    @GetMapping("/purchase-orders/waiting-receiving") public List<PurchaseOrderResponse> waiting(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int limit) { return service.orderList(page,limit,null,"Confirmed"); }
}
