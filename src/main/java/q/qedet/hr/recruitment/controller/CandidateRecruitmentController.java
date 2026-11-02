package q.qedet.hr.recruitment.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import q.qedet.hr.recruitment.dto.ApplicationResponse;
import q.qedet.hr.recruitment.dto.CandidateCvRequest;
import q.qedet.hr.recruitment.dto.CandidateCvResponse;
import q.qedet.hr.recruitment.dto.CandidateProfileRequest;
import q.qedet.hr.recruitment.dto.CandidateResponse;
import q.qedet.hr.recruitment.dto.OfferResponse;
import q.qedet.hr.recruitment.dto.OfferStatusRequest;
import q.qedet.hr.recruitment.service.RecruitmentService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recruitment")
public class CandidateRecruitmentController {
    private final RecruitmentService service;

    public CandidateRecruitmentController(RecruitmentService service) {
        this.service = service;
    }

    @GetMapping("/candidates/me")
    public ResponseEntity<CandidateResponse> myProfile() {
        return ResponseEntity.ok(service.myCandidateProfile());
    }

    @PostMapping("/candidates")
    public ResponseEntity<CandidateResponse> createMyProfile(@Valid @RequestBody CandidateProfileRequest request) {
        return ResponseEntity.ok(service.createMyCandidateProfile(request));
    }

    @PutMapping("/candidates/me")
    public ResponseEntity<CandidateResponse> updateMyProfile(@Valid @RequestBody CandidateProfileRequest request) {
        return ResponseEntity.ok(service.updateMyCandidateProfile(request));
    }

    @GetMapping("/candidates/me/cv")
    public ResponseEntity<CandidateCvResponse> myCv() {
        return ResponseEntity.ok(service.myCv());
    }

    @PutMapping("/candidates/me/cv")
    public ResponseEntity<CandidateCvResponse> saveMyCv(@Valid @RequestBody CandidateCvRequest request) {
        return ResponseEntity.ok(service.saveMyCv(request));
    }

    @PostMapping(value = "/candidates/me/cv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CandidateCvResponse> uploadMyCv(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(service.uploadMyCv(file));
    }

    @GetMapping("/candidates/me/cv/download")
    public ResponseEntity<CandidateCvResponse> downloadMyCv() {
        return ResponseEntity.ok(service.myCv());
    }

    @DeleteMapping("/candidates/me/cv")
    public ResponseEntity<Void> deleteMyCv() {
        service.deleteMyCv();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/applications/me")
    public ResponseEntity<List<ApplicationResponse>> myApplications() {
        return ResponseEntity.ok(service.myApplications());
    }

    @PatchMapping("/applications/{id}/withdraw")
    public ResponseEntity<ApplicationResponse> withdraw(@PathVariable UUID id) {
        return ResponseEntity.ok(service.withdrawMyApplication(id));
    }

    @GetMapping("/offers/me")
    public ResponseEntity<List<OfferResponse>> myOffers() {
        return ResponseEntity.ok(service.myOffers());
    }

    @GetMapping("/offers/me/{id}")
    public ResponseEntity<OfferResponse> myOffer(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getMyOffer(id));
    }

    @PatchMapping("/offers/me/{id}/status")
    public ResponseEntity<OfferResponse> respondToOffer(
            @PathVariable UUID id, @Valid @RequestBody OfferStatusRequest request) {
        return ResponseEntity.ok(service.respondToMyOffer(id, request.status()));
    }

    @PostMapping("/offers/{id}/accept")
    public ResponseEntity<OfferResponse> acceptOffer(@PathVariable UUID id) {
        return ResponseEntity.ok(service.respondToMyOffer(id, q.qedet.hr.recruitment.model.OfferStatus.ACCEPTED));
    }

    @PostMapping("/offers/{id}/reject")
    public ResponseEntity<OfferResponse> rejectOffer(@PathVariable UUID id) {
        return ResponseEntity.ok(service.respondToMyOffer(id, q.qedet.hr.recruitment.model.OfferStatus.DECLINED));
    }
}
