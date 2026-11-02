package q.qedet.hr.document.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import q.qedet.hr.document.dto.DocumentUploadRequest;
import q.qedet.hr.document.entity.EmployeeDocument;
import q.qedet.hr.document.service.EmployeeDocumentService;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/hr/documents")
public class EmployeeDocumentController {
    private final EmployeeDocumentService service;
    public EmployeeDocumentController(EmployeeDocumentService service) { this.service = service; }
    @PostMapping("/employees/{employeeId}") public EmployeeDocument upload(@PathVariable UUID employeeId, @Valid @RequestBody DocumentUploadRequest request) {
        return service.upload(employeeId, request);
    }
    @PostMapping("/{id}/verify") public EmployeeDocument verify(@PathVariable UUID id) { return service.verify(id); }
    @PostMapping("/{id}/reject") public EmployeeDocument reject(@PathVariable UUID id, @RequestParam(required = false) String reason) {
        return service.reject(id, reason);
    }
}
