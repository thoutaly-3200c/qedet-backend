package q.qedet.warehouse.controller;
import jakarta.validation.Valid; import org.springframework.web.bind.annotation.*; import q.qedet.warehouse.dto.ReceivingDtos; import q.qedet.warehouse.entity.ReceivingRequest; import q.qedet.warehouse.service.ReceivingService; import java.util.*;
@RestController @RequestMapping("/api/v1/warehouse") public class ReceivingController {
 private final ReceivingService service; public ReceivingController(ReceivingService service){this.service=service;}
 @GetMapping("/receiving-requests") public List<ReceivingDtos.RequestResponse> list(@RequestParam(required=false) ReceivingRequest.Status status,@RequestParam(required=false) UUID warehouseId,@RequestParam(required=false) String purchaseOrderNumber){return service.list(status,warehouseId,purchaseOrderNumber);}
 @GetMapping("/receiving-requests/{id}") public ReceivingDtos.RequestResponse get(@PathVariable UUID id){return service.get(id);}
 @PostMapping("/receiving-requests/{id}/operations") public ReceivingDtos.OperationResponse start(@PathVariable UUID id,@RequestBody(required=false) ReceivingDtos.StartOperationRequest body){return service.start(id,body);}
 @GetMapping("/receiving-operations/{id}") public ReceivingDtos.OperationResponse operation(@PathVariable UUID id){return service.getOperation(id);}
 @PostMapping("/receiving-operations/{id}/supplier-qr") public ReceivingDtos.SupplierQrResolveResponse qr(@PathVariable UUID id,@Valid @RequestBody ReceivingDtos.SupplierQrResolveRequest body){return service.resolveSupplierQr(id,body);}
 @PostMapping("/receiving-operations/{id}/supplier-qr/decision") public ReceivingDtos.SupplierQrResolveResponse qrDecision(@PathVariable UUID id,@Valid @RequestBody ReceivingDtos.SupplierQrDecisionRequest body){return service.decideSupplierQr(id,body);}
 @PutMapping("/receiving-operations/{id}/items") public ReceivingDtos.OperationResponse item(@PathVariable UUID id,@Valid @RequestBody ReceivingDtos.OperationItemRequest body){return service.record(id,body);}
 @PostMapping("/receiving-operations/{id}/complete") public ReceivingDtos.OperationResponse complete(@PathVariable UUID id){return service.complete(id);}
 @PostMapping("/receiving-operations/{id}/cancel") public ReceivingDtos.OperationResponse cancel(@PathVariable UUID id){return service.cancel(id);}
 @GetMapping("/receiving-request-items/{id}/history") public ReceivingDtos.HistoryResponse history(@PathVariable UUID id){return service.history(id);}
}
