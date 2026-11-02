package q.qedet.organization.controller;
import jakarta.validation.Valid; import org.springframework.web.bind.annotation.*; import q.qedet.organization.dto.OrganizationZoneRequest; import q.qedet.organization.entity.OrganizationZone; import q.qedet.organization.service.OrganizationZoneService;

import java.util.List;
import java.util.UUID;
@RestController @RequestMapping("/api/v1/organizations/{organizationId}/sites/{siteId}/zones")
public class OrganizationZoneController {
 private final OrganizationZoneService service; public OrganizationZoneController(OrganizationZoneService service){this.service=service;}
 @PostMapping public OrganizationZone create(@PathVariable UUID organizationId,@PathVariable UUID siteId,@Valid @RequestBody OrganizationZoneRequest request){return service.create(organizationId,siteId,request);}

 @GetMapping
 public List<OrganizationZone> list(
         @PathVariable UUID organizationId,
         @PathVariable UUID siteId
 ) {
  return service.list(organizationId, siteId);
 }
 @PatchMapping("/{id}") public OrganizationZone update(@PathVariable UUID organizationId,@PathVariable UUID siteId,@PathVariable UUID id,@Valid @RequestBody OrganizationZoneRequest request){return service.update(organizationId,siteId,id,request);}
}
