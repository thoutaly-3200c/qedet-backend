package q.qedet.organization.controller;
import jakarta.validation.Valid; import org.springframework.web.bind.annotation.*; import q.qedet.organization.dto.OrganizationBinRequest; import q.qedet.organization.entity.OrganizationBin; import q.qedet.organization.service.OrganizationBinService; import java.util.UUID;
@RestController @RequestMapping("/api/v1/organizations/{organizationId}/zones/{zoneId}/bins")
public class OrganizationBinController {
 private final OrganizationBinService service; public OrganizationBinController(OrganizationBinService service){this.service=service;}
 @PostMapping public OrganizationBin create(@PathVariable UUID organizationId,@PathVariable UUID zoneId,@Valid @RequestBody OrganizationBinRequest request){return service.create(organizationId,zoneId,request);}
}
