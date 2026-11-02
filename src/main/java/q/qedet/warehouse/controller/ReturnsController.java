package q.qedet.warehouse.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import q.qedet.warehouse.dto.ReturnDtos;
import q.qedet.warehouse.service.ReturnsService;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/warehouse/returns")
public class ReturnsController {
 private final ReturnsService service; public ReturnsController(ReturnsService service) { this.service = service; }
 @GetMapping public ReturnDtos.PageResponse list(@RequestParam(required=false) String type,@RequestParam(required=false) String status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){ return service.list(type,status,page,size); }
 @GetMapping("/summary") public ReturnDtos.Summary summary(){ return service.summary(); }
 @GetMapping("/{returnId}") public ReturnDtos.ReturnResponse get(@PathVariable UUID returnId){ return service.get(returnId); }
 @PostMapping("/scan") public ReturnDtos.ScanResponse scan(@Valid @RequestBody ReturnDtos.ScanRequest body){ return service.scan(body); }
 @PostMapping("/validate-receipt") public ReturnDtos.ReceiptValidationResponse validateReceipt(@Valid @RequestBody ReturnDtos.ReceiptValidationRequest body){ return service.validateReceipt(body); }
 @PostMapping("/customer") public ReturnDtos.ReturnResponse customer(@Valid @RequestBody ReturnDtos.CustomerCreate body){ return service.customer(body); }
 @PostMapping("/supplier") public ReturnDtos.ReturnResponse supplier(@Valid @RequestBody ReturnDtos.SupplierCreate body){ return service.supplier(body); }
 @PostMapping("/damage") public ReturnDtos.ReturnResponse damage(@Valid @RequestBody ReturnDtos.DamageCreate body){ return service.damage(body); }
 @PostMapping("/{returnId}/receiving") public ReturnDtos.ReceivingResponse receiving(@PathVariable UUID returnId){ return service.startReceiving(returnId); }
 @PostMapping("/{returnId}/receiving/scan") public ReturnDtos.ReturnResponse receiveScan(@PathVariable UUID returnId,@Valid @RequestBody ReturnDtos.ReceivingScanRequest body){ return service.receiveScan(returnId,body); }
 @PostMapping("/{returnId}/inspection/start") public ReturnDtos.ReturnResponse startInspection(@PathVariable UUID returnId){ return service.startInspection(returnId); }
 @PostMapping("/{returnId}/inspection/decision") public ReturnDtos.ReturnResponse decision(@PathVariable UUID returnId,@Valid @RequestBody ReturnDtos.DecisionRequest body){ return service.decision(returnId,body); }
 @PostMapping("/{returnId}/financial-requests") public ReturnDtos.FinancialResponse financial(@PathVariable UUID returnId,@Valid @RequestBody ReturnDtos.FinancialRequest body){ return service.financial(returnId,body); }
 @PostMapping("/supplier/{returnId}/items/{itemId}/inspection-requests") public ReturnDtos.ReturnResponse supplierInspection(@PathVariable UUID returnId,@PathVariable UUID itemId,@Valid @RequestBody ReturnDtos.SupplierInspectionRequest body){ return service.supplierInspection(returnId,itemId,body); }
}
