package q.qedet.hr.recruitment.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import q.qedet.hr.recruitment.dto.ApplicationResponse;
import q.qedet.hr.recruitment.dto.EmployeeApplicationRequest;
import q.qedet.hr.recruitment.service.RecruitmentService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobApplicationController {
    private final RecruitmentService service;

    public JobApplicationController(RecruitmentService service) {
        this.service = service;
    }

    @PostMapping("/{jobId}/apply")
    public ResponseEntity<ApplicationResponse> apply(
            @PathVariable UUID jobId,
            @Valid @RequestBody EmployeeApplicationRequest request
    ) {
        return ResponseEntity.ok(service.applyAsEmployee(jobId, request));
    }

    @PostMapping("/{jobId}/apply-as-candidate")
    public ResponseEntity<ApplicationResponse> applyAsCandidate(
            @PathVariable UUID jobId,
            @Valid @RequestBody EmployeeApplicationRequest request
    ) {
        return ResponseEntity.ok(service.applyAsCandidate(jobId, request));
    }
}
