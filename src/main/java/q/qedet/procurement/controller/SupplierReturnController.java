package q.qedet.procurement.controller;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import q.qedet.procurement.dto.SupplierReturnDtos.*;
import q.qedet.procurement.service.SupplierReturnService;
import q.qedet.auth.security.ActorContext;
import q.qedet.common.exception.NotFoundException;
import java.util.*;
@RestController @RequestMapping("/api/v1/organizations/{organizationId}/procurement")
public class SupplierReturnController {
 private final SupplierReturnService service; private final ActorContext actor;
 public SupplierReturnController(SupplierReturnService service,ActorContext actor){this.service=service;this.actor=actor;}
 private void organization(UUID id){if(!actor.organizationId().equals(id))throw new NotFoundException("ORGANIZATION_NOT_FOUND");}
 @GetMapping("/purchase-returns") public Map<String,Object> requests(@PathVariable UUID organizationId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){organization(organizationId);return envelope(service.requests(page,size));}
 @GetMapping("/purchase-returns/{requestId}") public Request request(@PathVariable UUID organizationId,@PathVariable UUID requestId){organization(organizationId);return service.requestDetail(requestId);}
 @GetMapping("/purchase-returns/{requestId}/items/{itemId}/suppliers") public List<History> suppliers(@PathVariable UUID organizationId,@PathVariable UUID requestId,@PathVariable UUID itemId){organization(organizationId);return service.history(requestId,itemId);}
 @PostMapping("/purchase-returns/{requestId}/review") public List<Operation> review(@PathVariable UUID organizationId,@PathVariable UUID requestId,@RequestBody Review body){organization(organizationId);return service.review(requestId,body);}
 @GetMapping("/supplier-returns") public Map<String,Object> operations(@PathVariable UUID organizationId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){organization(organizationId);return envelope(service.operations(page,size));}
 @GetMapping("/supplier-returns/{operationId}") public Detail operation(@PathVariable UUID organizationId,@PathVariable UUID operationId){organization(organizationId);return service.detail(operationId);}
 @PostMapping("/supplier-returns/{operationId}/dispatch") public Operation dispatch(@PathVariable UUID organizationId,@PathVariable UUID operationId,@RequestBody Dispatch body){organization(organizationId);return service.dispatch(operationId,body);}
 @PostMapping("/supplier-returns/{operationId}/supplier-acceptance") public Operation acceptance(@PathVariable UUID organizationId,@PathVariable UUID operationId,@RequestBody Acceptance body){organization(organizationId);return service.acceptance(operationId,body);}
 @PostMapping("/supplier-returns/{operationId}/settlement") public Operation settlement(@PathVariable UUID organizationId,@PathVariable UUID operationId,@RequestBody Settlement body){organization(organizationId);return service.settlement(operationId,body);}
 private Map<String,Object> envelope(Page<?> result){Map<String,Object> response=new LinkedHashMap<>();response.put("items",result.getContent());response.put("total",result.getTotalElements());response.put("page",result.getNumber());response.put("size",result.getSize());return response;}
}
