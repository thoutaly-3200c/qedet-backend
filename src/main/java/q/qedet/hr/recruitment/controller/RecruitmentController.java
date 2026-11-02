package q.qedet.hr.recruitment.controller;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import q.qedet.employee.dto.EmployeeInvitationRequest;
import q.qedet.employee.dto.EmployeeResponse;
import q.qedet.employee.service.EmployeeService;
import q.qedet.hr.recruitment.dto.*;
import q.qedet.hr.recruitment.entity.*;
import q.qedet.hr.recruitment.service.RecruitmentService;
import java.util.List;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/recruitment")
public class RecruitmentController {
    private final RecruitmentService service;
    private final EmployeeService employeeService;

    public RecruitmentController(RecruitmentService service, EmployeeService employeeService) {
        this.service = service;
        this.employeeService = employeeService;
    }
    @PostMapping(value = "/jobs", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JobOpeningResponse> createJob(@Valid @RequestBody CreateJobRequest request) {
        return ResponseEntity.ok(service.createJob(request));
    }
    @GetMapping("/jobs")
    public ResponseEntity<List<JobOpeningResponse>> listJobs() {
        return ResponseEntity.ok(service.listJobs());
    }
    @GetMapping("/jobs/{id}")
    public ResponseEntity<JobOpeningResponse> getJob(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getJob(id));
    }
    @PostMapping("/jobs/{id}/publish")
    public ResponseEntity<JobOpeningResponse> publishJob(@PathVariable UUID id) {
        return ResponseEntity.ok(service.publishJob(id));
    }
    @PostMapping("/jobs/{jobId}/applications")
    public Application apply(@PathVariable UUID jobId, @RequestParam String firstName, @RequestParam String lastName,
                             @RequestParam String email, @RequestParam(required = false) String resumeKey) {
        return service.apply(jobId, firstName, lastName, email, resumeKey);
    }
    @PostMapping("/applications/{id}/shortlist") public Application shortlist(@PathVariable UUID id) { return service.shortlist(id); }
    @PostMapping("/applications/{id}/reject") public Application reject(@PathVariable UUID id) { return service.reject(id); }

    @GetMapping("/applications")
    public ResponseEntity<List<ApplicationResponse>> listApplications(
            @RequestParam(required = false) UUID jobId,
            @RequestParam(required = false) UUID candidateId,
            @RequestParam(required = false) q.qedet.hr.recruitment.model.ApplicationStatus status) {
        return ResponseEntity.ok(service.listApplications(jobId, candidateId, status));
    }

    @GetMapping("/jobs/{jobId}/applications")
    public ResponseEntity<List<ApplicationResponse>> listJobApplications(@PathVariable UUID jobId) {
        return ResponseEntity.ok(service.listApplications(jobId));
    }

    @GetMapping("/applications/{id}")
    public ResponseEntity<ApplicationResponse> getApplication(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getApplication(id));
    }

    @GetMapping("/interviewers")
    public ResponseEntity<List<EmployeeResponse>> listInterviewers() {
        return ResponseEntity.ok(service.listInterviewers());
    }

    @PatchMapping("/applications/{id}/status")
    public ResponseEntity<ApplicationResponse> updateApplicationStatus(
            @PathVariable UUID id, @Valid @RequestBody ApplicationStatusRequest request) {
        return ResponseEntity.ok(service.updateApplicationStatus(id, request.status()));
    }

    @GetMapping("/candidates")
    public ResponseEntity<List<CandidateResponse>> listCandidates(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean archived) {
        return ResponseEntity.ok(service.listCandidates(search, archived));
    }

    @GetMapping("/candidates/{candidateId}")
    public ResponseEntity<CandidateResponse> getCandidate(@PathVariable UUID candidateId) {
        return ResponseEntity.ok(service.getCandidate(candidateId));
    }

    @PatchMapping("/candidates/{candidateId}")
    public ResponseEntity<CandidateResponse> updateCandidate(
            @PathVariable UUID candidateId, @Valid @RequestBody CandidateProfileRequest request) {
        return ResponseEntity.ok(service.updateCandidate(candidateId, request));
    }

    @GetMapping("/candidates/{candidateId}/cv")
    public ResponseEntity<CandidateCvResponse> getCandidateCv(@PathVariable UUID candidateId) {
        return ResponseEntity.ok(service.candidateCv(candidateId));
    }

    @PostMapping(value = "/candidates/{candidateId}/cv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CandidateCvResponse> uploadCandidateCv(
            @PathVariable UUID candidateId, @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(service.uploadCandidateCv(candidateId, file));
    }

    @GetMapping("/candidates/{candidateId}/cv/download")
    public ResponseEntity<CandidateCvResponse> downloadCandidateCv(@PathVariable UUID candidateId) {
        return ResponseEntity.ok(service.candidateCv(candidateId));
    }

    @PostMapping("/applications/{applicationId}/interviews")
    public ResponseEntity<InterviewResponse> scheduleInterview(
            @PathVariable UUID applicationId, @Valid @RequestBody InterviewRequest request) {
        return ResponseEntity.ok(service.scheduleInterview(applicationId, request));
    }

    @GetMapping("/applications/{applicationId}/interviews")
    public ResponseEntity<List<InterviewResponse>> listInterviews(@PathVariable UUID applicationId) {
        return ResponseEntity.ok(service.listInterviews(applicationId));
    }

    @GetMapping("/interviews/{id}")
    public ResponseEntity<InterviewResponse> getInterview(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getInterview(id));
    }

    @PutMapping("/interviews/{id}")
    public ResponseEntity<InterviewResponse> updateInterview(
            @PathVariable UUID id, @Valid @RequestBody InterviewRequest request) {
        return ResponseEntity.ok(service.updateInterview(id, request));
    }

    @PatchMapping("/interviews/{id}")
    public ResponseEntity<InterviewResponse> patchInterview(
            @PathVariable UUID id, @Valid @RequestBody InterviewRequest request) {
        return ResponseEntity.ok(service.updateInterview(id, request));
    }

    @PostMapping("/interviews/{id}/reschedule")
    public ResponseEntity<InterviewResponse> rescheduleInterview(
            @PathVariable UUID id, @Valid @RequestBody InterviewRequest request) {
        return ResponseEntity.ok(service.rescheduleInterview(id, request));
    }

    @PostMapping("/interviews/{id}/cancel")
    public ResponseEntity<InterviewResponse> cancelInterview(@PathVariable UUID id) {
        return ResponseEntity.ok(service.cancelInterview(id));
    }

    @PostMapping("/interviews/{id}/result")
    public ResponseEntity<InterviewResponse> recordInterviewResult(
            @PathVariable UUID id, @Valid @RequestBody InterviewResultRequest request) {
        return ResponseEntity.ok(service.recordInterviewResult(id, request));
    }

    @PatchMapping("/interviews/{id}/status")
    public ResponseEntity<InterviewResponse> updateInterviewStatus(
            @PathVariable UUID id, @Valid @RequestBody InterviewStatusRequest request) {
        return ResponseEntity.ok(service.updateInterviewStatus(id, request));
    }

    @PostMapping("/applications/{applicationId}/offers")
    public ResponseEntity<OfferResponse> createOffer(
            @PathVariable UUID applicationId, @Valid @RequestBody OfferRequest request) {
        return ResponseEntity.ok(service.createOffer(applicationId, request));
    }

    @PostMapping("/applications/{applicationId}/offer")
    public ResponseEntity<OfferResponse> createApplicationOffer(
            @PathVariable UUID applicationId, @Valid @RequestBody OfferRequest request) {
        return ResponseEntity.ok(service.createOffer(applicationId, request));
    }

    @GetMapping("/applications/{applicationId}/offers")
    public ResponseEntity<List<OfferResponse>> listOffers(@PathVariable UUID applicationId) {
        return ResponseEntity.ok(service.listOffers(applicationId));
    }

    @GetMapping("/applications/{applicationId}/offer")
    public ResponseEntity<OfferResponse> getApplicationOffer(@PathVariable UUID applicationId) {
        return ResponseEntity.ok(service.applicationOffer(applicationId));
    }

    @GetMapping("/offers/{id}")
    public ResponseEntity<OfferResponse> getOffer(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getOffer(id));
    }

    @PutMapping("/offers/{id}")
    public ResponseEntity<OfferResponse> updateOffer(
            @PathVariable UUID id, @Valid @RequestBody OfferRequest request) {
        return ResponseEntity.ok(service.updateOffer(id, request));
    }

    @PatchMapping("/offers/{id}")
    public ResponseEntity<OfferResponse> patchOffer(
            @PathVariable UUID id, @Valid @RequestBody OfferRequest request) {
        return ResponseEntity.ok(service.updateOffer(id, request));
    }

    @PatchMapping("/offers/{id}/status")
    public ResponseEntity<OfferResponse> updateOfferStatus(
            @PathVariable UUID id, @Valid @RequestBody OfferStatusRequest request) {
        return ResponseEntity.ok(service.updateOfferStatus(id, request.status()));
    }

    @PostMapping("/employees/invite")
    public ResponseEntity<EmployeeResponse> inviteEmployee(@Valid @RequestBody EmployeeInvitationRequest request) {
        return ResponseEntity.ok(employeeService.inviteNew(request));
    }

        @PostMapping("/applications/{applicationId}/employee-invitation")
        public ResponseEntity<EmployeeResponse> inviteApplicationCandidate(@PathVariable UUID applicationId) {
            return ResponseEntity.ok(employeeService.inviteNew(service.employeeInvitation(applicationId)));
        }
}
