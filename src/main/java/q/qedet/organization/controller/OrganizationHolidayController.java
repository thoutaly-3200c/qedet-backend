package q.qedet.organization.controller;
import jakarta.validation.Valid; import org.springframework.web.bind.annotation.*; import q.qedet.organization.policy.dto.OrganizationHolidayDtos.*; import q.qedet.organization.policy.service.OrganizationHolidayService; import java.util.*;
@RestController @RequestMapping("/api/v1/organization/holidays") public class OrganizationHolidayController {
 private final OrganizationHolidayService service; public OrganizationHolidayController(OrganizationHolidayService service){this.service=service;}
 @GetMapping public List<Response> list(){return service.list();} @PostMapping public Response create(@Valid @RequestBody Request r){return service.create(r);}
 @PatchMapping("/{id}") public Response update(@PathVariable UUID id,@Valid @RequestBody Request r){return service.update(id,r);}
 @RequestMapping(value="/{id}",method={RequestMethod.DELETE,RequestMethod.POST}) public Response archive(@PathVariable UUID id){return service.archive(id);}
}
