package q.qedet.warehouse.controller;
import jakarta.validation.Valid; import org.springframework.web.bind.annotation.*; import q.qedet.warehouse.dto.FulfillmentDtos; import q.qedet.warehouse.service.SalesFulfillmentService; import java.util.*;
@RestController @RequestMapping("/api/v1/warehouse/sales-stock-requests")
public class SalesFulfillmentController { private final SalesFulfillmentService service; public SalesFulfillmentController(SalesFulfillmentService service){this.service=service;}
 @PostMapping("/{requestId}/accept") public Map<String,Object> acceptRequest(@PathVariable UUID requestId){return service.acceptRequest(requestId);}
 @GetMapping("/{requestId}/fulfillments") public List<Map<String,Object>> list(@PathVariable UUID requestId){return service.list(requestId);}
 @PostMapping("/{requestId}/fulfillments") public Map<String,Object> confirm(@PathVariable UUID requestId,@Valid @RequestBody FulfillmentDtos.Confirm body){return service.confirm(requestId,body);}
 @PostMapping("/fulfillments/{id}/reject") public Map<String,Object> reject(@PathVariable UUID id,@Valid @RequestBody FulfillmentDtos.Decision body){return service.reject(id,body);}
 @PostMapping("/fulfillments/{id}/return-receipt") public Map<String,Object> returned(@PathVariable UUID id,@Valid @RequestBody FulfillmentDtos.ReturnReceipt body){return service.receivePhysicalReturn(id,body);}}
