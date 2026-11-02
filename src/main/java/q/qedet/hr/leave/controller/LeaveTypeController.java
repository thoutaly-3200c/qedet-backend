package q.qedet.hr.leave.controller;
import jakarta.validation.Valid; import org.springframework.web.bind.annotation.*; import q.qedet.hr.leave.dto.LeaveTypeDtos.*; import q.qedet.hr.leave.service.LeaveTypeService; import java.util.*;
@RestController @RequestMapping("/api/v1/leave/types") public class LeaveTypeController {
 private final LeaveTypeService service; public LeaveTypeController(LeaveTypeService service){this.service=service;}
 @GetMapping public List<Response> list(){return service.list();} @PostMapping public Response create(@Valid @RequestBody Request r){return service.create(r);}
 @GetMapping("/{id}") public Response get(@PathVariable UUID id){return service.get(id);} @PatchMapping("/{id}") public Response update(@PathVariable UUID id,@Valid @RequestBody Request r){return service.update(id,r);}
 @RequestMapping(value="/{id}",method={RequestMethod.DELETE,RequestMethod.POST}) public Response archive(@PathVariable UUID id){return service.archive(id);}
}
