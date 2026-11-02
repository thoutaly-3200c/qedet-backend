package q.qedet.hr.onboarding.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import q.qedet.hr.onboarding.dto.OnboardingTaskRequest;
import q.qedet.hr.onboarding.entity.*;
import q.qedet.hr.onboarding.service.OnboardingService;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/hr/onboarding")
public class OnboardingController {
    private final OnboardingService service;
    public OnboardingController(OnboardingService service) { this.service = service; }
    @PostMapping("/{employeeId}") public Onboarding create(@PathVariable UUID employeeId) { return service.create(employeeId); }
    @PostMapping("/{onboardingId}/tasks") public OnboardingTask addTask(@PathVariable UUID onboardingId, @Valid @RequestBody OnboardingTaskRequest request) {
        return service.addTask(onboardingId, request);
    }
    @PostMapping("/tasks/{taskId}/complete") public OnboardingTask completeTask(@PathVariable UUID taskId) { return service.completeTask(taskId); }
}
