package q.qedet.organization.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import q.qedet.organization.policy.dto.WorkScheduleRequest;
import q.qedet.organization.policy.dto.WorkScheduleResponse;
import q.qedet.organization.policy.service.AttendanceScheduleService;
import java.util.*;

@RestController
@RequestMapping("/api/v1/organization/work-schedules")
public class OrganizationWorkScheduleController {
 private final AttendanceScheduleService service;
 public OrganizationWorkScheduleController(AttendanceScheduleService service){this.service=service;}
 @GetMapping public List<WorkScheduleResponse> list(){return service.listForCurrentOrganization();}
 @PostMapping public WorkScheduleResponse create(@Valid @RequestBody WorkScheduleRequest r){return service.createForCurrentOrganization(r);}
 @GetMapping("/{id}") public WorkScheduleResponse get(@PathVariable UUID id){return service.getForCurrentOrganization(id);}
 @PatchMapping("/{id}") public WorkScheduleResponse update(@PathVariable UUID id,@Valid @RequestBody WorkScheduleRequest r){return service.updateForCurrentOrganization(id,r);}
 @PostMapping("/{id}/archive") public WorkScheduleResponse archive(@PathVariable UUID id){return service.archiveForCurrentOrganization(id);}
 @PostMapping("/{id}/restore") public WorkScheduleResponse restore(@PathVariable UUID id){return service.restoreForCurrentOrganization(id);}
}
