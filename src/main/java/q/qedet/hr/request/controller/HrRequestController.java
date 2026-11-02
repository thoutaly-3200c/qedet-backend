package q.qedet.hr.request.controller;

import org.springframework.web.bind.annotation.*;
import q.qedet.hr.request.entity.HrRequest;
import q.qedet.hr.request.model.HrRequestType;
import q.qedet.hr.request.service.HrRequestService;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/hr/requests")
public class HrRequestController {
    private final HrRequestService service;
    public HrRequestController(HrRequestService service) { this.service = service; }
    @PostMapping @ResponseBody public HrRequest create(@RequestParam UUID employeeId, @RequestParam HrRequestType type,
                                                        @RequestParam(required = false) String details) {
        return service.create(employeeId, type, details);
    }
    @PostMapping("/{id}/submit") public HrRequest submit(@PathVariable UUID id) { return service.submit(id); }
    @PostMapping("/{id}/approve") public HrRequest approve(@PathVariable UUID id) { return service.approve(id); }
    @PostMapping("/{id}/reject") public HrRequest reject(@PathVariable UUID id) { return service.reject(id); }
    @PostMapping("/{id}/execute") public HrRequest execute(@PathVariable UUID id) { return service.execute(id); }
}
