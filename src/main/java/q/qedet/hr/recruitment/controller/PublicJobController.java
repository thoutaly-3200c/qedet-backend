package q.qedet.hr.recruitment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import q.qedet.hr.recruitment.dto.JobOpeningResponse;
import q.qedet.hr.recruitment.service.RecruitmentService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
public class PublicJobController {
    private final RecruitmentService service;

    public PublicJobController(RecruitmentService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobOpeningResponse> getJob(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getPublicJob(id));
    }
}
