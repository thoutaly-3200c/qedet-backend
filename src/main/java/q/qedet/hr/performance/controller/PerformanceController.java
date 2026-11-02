package q.qedet.hr.performance.controller;

import org.springframework.web.bind.annotation.*;
import q.qedet.hr.performance.entity.Goal;
import q.qedet.hr.performance.service.PerformanceService;
import java.time.LocalDate;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/hr/performance")
public class PerformanceController {
    private final PerformanceService service;
    public PerformanceController(PerformanceService service) { this.service = service; }
    @PostMapping("/employees/{employeeId}/goals")
    public Goal createGoal(@PathVariable UUID employeeId, @RequestParam String title,
                           @RequestParam(required = false) String description,
                           @RequestParam(required = false) LocalDate dueDate) {
        return service.createGoal(employeeId, title, description, dueDate);
    }
    @PostMapping("/goals/{id}/complete")
    public Goal completeGoal(@PathVariable UUID id) { return service.completeGoal(id); }
}
