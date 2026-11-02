package q.qedet.organization.controller;
import jakarta.validation.Valid; import org.springframework.web.bind.annotation.*; import q.qedet.organization.dto.*; import q.qedet.organization.service.OrganizationSiteService; import java.util.*;
@RestController @RequestMapping("/api/v1/organizations/{organizationId}/sites")
public class OrganizationSiteController {
 private final OrganizationSiteService service; public OrganizationSiteController(OrganizationSiteService service){this.service=service;}
 @GetMapping public List<OrganizationSiteResponse> list(@PathVariable UUID organizationId){return service.list(organizationId);}
 @PostMapping public OrganizationSiteResponse create(@PathVariable UUID organizationId,@Valid @RequestBody OrganizationSiteRequest request){return service.create(organizationId,request);}
 @PatchMapping("/{id}") public OrganizationSiteResponse update(@PathVariable UUID organizationId,@PathVariable UUID id,@Valid @RequestBody OrganizationSiteRequest request){return service.update(organizationId,id,request);}
 @PostMapping("/{id}/activate") public OrganizationSiteResponse activate(@PathVariable UUID organizationId,@PathVariable UUID id){return service.activate(organizationId,id);}
 @PostMapping("/{id}/deactivate") public OrganizationSiteResponse deactivate(@PathVariable UUID organizationId,@PathVariable UUID id){return service.deactivate(organizationId,id);}
}
